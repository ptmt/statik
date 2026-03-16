package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.SiteGenerator
import java.nio.file.Path
import java.nio.file.Paths

class CmsService(
    private val rootPath: Path,
    private val config: BlogConfig,
    private val generator: SiteGenerator,
    private val contentFileService: ContentFileService = ContentFileService(rootPath, config),
    private val repository: CmsRepository = CmsRepository(resolveDatabasePath(rootPath, config.cms.databasePath)),
    private val gitSyncService: GitSyncService = GitSyncService(rootPath, config.cms.git)
) {
    private val lock = Any()
    private val basePath = normalizeBasePath(config.cms.basePath)

    init {
        repository.initialize()
    }

    fun bootstrap() {
        synchronized(lock) {
            refreshIndexLocked()
        }
    }

    fun list(type: CmsContentType? = null): CmsContentListResponse {
        val items = repository.list(type).map { it.toSummary() }
        return CmsContentListResponse(
            items = items,
            total = items.size,
            dirty = items.count { it.dirty }
        )
    }

    fun get(sourcePath: String): CmsContentDocument {
        val normalized = normalizeSourcePath(sourcePath)
        return repository.find(normalized)?.toDocument()
            ?: throw IllegalArgumentException("CMS content not found: $sourcePath")
    }

    fun save(request: CmsSaveRequest, accessToken: String? = null): CmsSaveResponse {
        synchronized(lock) {
            val normalizedSourcePath = normalizeSourcePath(request.sourcePath)
            val existing = repository.find(normalizedSourcePath)
            val saved = contentFileService.save(request.copy(sourcePath = normalizedSourcePath))
            val dirtyEntry = saved.copy(
                dirty = true,
                updatedAt = System.currentTimeMillis(),
                lastSyncedAt = existing?.lastSyncedAt
            )

            repository.upsert(dirtyEntry)
            generator.regenerate(listOf(rootPath.resolve(saved.sourcePath)))

            val syncResponse = if (request.sync || config.cms.autoSyncOnSave) {
                syncLocked(request.commitMessage, null, accessToken)
            } else {
                null
            }

            val stored = repository.find(saved.sourcePath)
                ?: throw IllegalStateException("Saved CMS content was not persisted: ${saved.sourcePath}")

            return CmsSaveResponse(
                item = stored.toDocument(),
                sync = syncResponse
            )
        }
    }

    fun refreshIndex(): CmsRefreshResponse {
        synchronized(lock) {
            return refreshIndexLocked()
        }
    }

    fun sync(commitMessage: String?, push: Boolean?, accessToken: String? = null): CmsSyncResponse {
        synchronized(lock) {
            return syncLocked(commitMessage, push, accessToken)
        }
    }

    fun status(): CmsStatusResponse {
        return CmsStatusResponse(
            enabled = true,
            basePath = basePath,
            items = repository.count(),
            dirty = repository.dirtyCount(),
            lastSyncedAt = repository.lastSyncedAt(),
            git = gitSyncService.status()
        )
    }

    private fun refreshIndexLocked(): CmsRefreshResponse {
        val scannedEntries = contentFileService.scanAll()
        repository.replaceFromScan(scannedEntries)
        return CmsRefreshResponse(
            items = repository.count(),
            dirty = repository.dirtyCount()
        )
    }

    private fun syncLocked(commitMessage: String?, push: Boolean?, accessToken: String?): CmsSyncResponse {
        val dirtyPaths = repository.dirtySourcePaths()
        val outcome = gitSyncService.sync(dirtyPaths, commitMessage, push, accessToken)

        if (outcome.committed) {
            repository.markSynced(dirtyPaths, System.currentTimeMillis())
        }

        return CmsSyncResponse(
            committed = outcome.committed,
            commitId = outcome.commitId,
            message = outcome.message,
            pushAttempted = outcome.pushAttempted,
            pushSucceeded = outcome.pushSucceeded,
            files = outcome.files,
            dirtyRemaining = repository.dirtyCount()
        )
    }

    companion object {
        private fun resolveDatabasePath(rootPath: Path, configuredPath: String): Path {
            val path = Paths.get(configuredPath)
            return if (path.isAbsolute) path else rootPath.resolve(path).normalize()
        }

        internal fun normalizeBasePath(basePath: String): String {
            val trimmed = basePath.trim().ifBlank { "/__statik__/cms" }
            return "/" + trimmed.removePrefix("/").removeSuffix("/")
        }

        private fun normalizeSourcePath(sourcePath: String): String {
            return sourcePath.trim().removePrefix("/").replace('\\', '/')
        }
    }
}
