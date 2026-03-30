package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.AssetManager
import com.potomushto.statik.generators.FileWalker
import com.potomushto.statik.generators.SiteGenerator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.Comparator

class CmsService(
    private val rootPath: Path,
    private val config: BlogConfig,
    private val generator: SiteGenerator,
    private val contentFileService: ContentFileService = ContentFileService(rootPath, config),
    private val mediaFileService: MediaFileService = MediaFileService(rootPath, config),
    databasePath: Path? = null,
    private val repository: CmsRepository = CmsRepository(databasePath ?: resolveDatabasePath(rootPath, config.cms.databasePath)),
    private val gitSyncService: GitSyncService = GitSyncService(rootPath, config.cms.git, config.theme.output),
    private val assetManager: AssetManager = AssetManager(rootPath.toString(), config, FileWalker(rootPath.toString()))
) {
    private val lock = Any()
    private val basePath = normalizeBasePath(config.cms.basePath)
    private val previewConfig = config.copy(
        theme = config.theme.copy(output = PREVIEW_OUTPUT_DIRECTORY)
    )
    private val previewGenerator = SiteGenerator(
        rootPath = rootPath.toString(),
        config = previewConfig,
        baseUrlOverride = "$basePath/preview/",
        isDevelopment = true
    )
    private val previewAssetManager = AssetManager(rootPath.toString(), previewConfig, FileWalker(rootPath.toString()))
    private var previewInitialized = false

    init {
        repository.initialize()
    }

    fun bootstrap() {
        synchronized(lock) {
            refreshIndexLocked()
        }
    }

    fun resolvePreviewFile(requestedPath: String): Path? {
        synchronized(lock) {
            ensurePreviewInitializedLocked()
            return resolveGeneratedFile(previewOutputRoot(), requestedPath)
        }
    }

    fun list(type: CmsContentType? = null): CmsContentListResponse {
        val items = repository.list(type)
            .sortedWith(contentOrdering())
            .map { it.toSummary() }
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

    fun listMedia(): CmsMediaListResponse {
        val items = repository.listMedia()
            .mapNotNull { entry ->
                assetManager.publicPathForSourcePath(entry.sourcePath)?.let(entry::toItem)
            }

        return CmsMediaListResponse(
            items = items,
            roots = mediaFileService.managedRoots(),
            total = items.size,
            dirty = repository.mediaDirtyCount()
        )
    }

    fun save(request: CmsSaveRequest, accessToken: String? = null): CmsSaveResponse {
        synchronized(lock) {
            val normalizedSourcePath = normalizeSourcePath(request.sourcePath)
            val normalizedPreviousSourcePath = request.previousSourcePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::normalizeSourcePath)

            val previousEntry = normalizedPreviousSourcePath?.let(repository::find)
            if (previousEntry != null && previousEntry.sourcePath != normalizedSourcePath) {
                contentFileService.rename(previousEntry.sourcePath, normalizedSourcePath, previousEntry.type)
            }

            val existing = repository.find(normalizedSourcePath) ?: previousEntry
            val saved = contentFileService.save(
                request.copy(
                    sourcePath = normalizedSourcePath,
                    previousSourcePath = normalizedPreviousSourcePath
                )
            )
            val dirtyEntry = saved.copy(
                dirty = true,
                updatedAt = System.currentTimeMillis(),
                lastSyncedAt = existing?.lastSyncedAt
            )

            if (previousEntry != null && previousEntry.sourcePath != normalizedSourcePath) {
                markContentDeleted(previousEntry.sourcePath, previousEntry.lastSyncedAt, previousEntry.dirty)
                if (previousEntry.outputPath != saved.outputPath) {
                    deleteGeneratedContent(previousEntry.outputPath)
                    if (previewInitialized) {
                        deleteGeneratedPreviewContent(previousEntry.outputPath)
                    }
                }
            }

            repository.upsert(dirtyEntry)
            generator.regenerate(listOf(rootPath.resolve(saved.sourcePath)))
            if (saved.type == CmsContentType.POST && isDraft(saved.metadata)) {
                deleteGeneratedContent(saved.outputPath)
            }
            if (previewInitialized) {
                previewGenerator.regenerate(listOf(rootPath.resolve(saved.sourcePath)))
            }

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

    fun uploadMedia(request: CmsMediaUploadRequest): CmsMediaMutationResponse {
        synchronized(lock) {
            val targetDirectory = request.targetDirectory.trim().ifBlank {
                mediaFileService.managedRoots().firstOrNull()
                    ?: throw IllegalStateException("No asset directories configured")
            }
            val normalizedRequest = request.copy(targetDirectory = targetDirectory)
            val existing = repository.findMedia(composeMediaPath(targetDirectory, request.fileName))
            val uploaded = mediaFileService.upload(normalizedRequest)
            assetManager.copySingleAsset(rootPath.resolve(uploaded.sourcePath).normalize())
            if (previewInitialized) {
                previewAssetManager.copySingleAsset(rootPath.resolve(uploaded.sourcePath).normalize())
            }
            repository.upsertMedia(
                uploaded.copy(
                    dirty = true,
                    deleted = false,
                    updatedAt = System.currentTimeMillis(),
                    lastSyncedAt = existing?.lastSyncedAt
                )
            )

            return CmsMediaMutationResponse(
                message = "Uploaded ${uploaded.fileName}.",
                selectedPath = uploaded.sourcePath,
                affectedPaths = listOf(uploaded.sourcePath)
            )
        }
    }

    fun renameMedia(request: CmsMediaRenameRequest): CmsMediaMutationResponse {
        synchronized(lock) {
            val mutation = mediaFileService.rename(request.sourcePath, request.targetPath)
            val renamedDirectory = mutation.updatedEntries.none { it.sourcePath == request.targetPath }

            mutation.deletedPaths.forEach { sourcePath ->
                markMediaDeleted(sourcePath)
                assetManager.deleteSingleAsset(sourcePath)
                if (previewInitialized) {
                    previewAssetManager.deleteSingleAsset(sourcePath)
                }
            }

            mutation.updatedEntries.forEach { entry ->
                val existing = repository.findMedia(entry.sourcePath)
                repository.upsertMedia(
                    entry.copy(
                        dirty = true,
                        deleted = false,
                        updatedAt = System.currentTimeMillis(),
                        lastSyncedAt = existing?.lastSyncedAt
                    )
                )
                assetManager.copySingleAsset(rootPath.resolve(entry.sourcePath).normalize())
                if (previewInitialized) {
                    previewAssetManager.copySingleAsset(rootPath.resolve(entry.sourcePath).normalize())
                }
            }

            return CmsMediaMutationResponse(
                message = if (renamedDirectory) {
                    "Renamed folder with ${mutation.updatedEntries.size} file(s)."
                } else {
                    "Renamed ${request.sourcePath.substringAfterLast('/')}."
                },
                selectedPath = request.targetPath,
                affectedPaths = mutation.deletedPaths + mutation.updatedEntries.map { it.sourcePath }
            )
        }
    }

    fun deleteMedia(request: CmsMediaDeleteRequest): CmsMediaMutationResponse {
        synchronized(lock) {
            val mutation = mediaFileService.delete(request.sourcePath)
            mutation.deletedPaths.forEach { sourcePath ->
                markMediaDeleted(sourcePath)
                assetManager.deleteSingleAsset(sourcePath)
                if (previewInitialized) {
                    previewAssetManager.deleteSingleAsset(sourcePath)
                }
            }

            return CmsMediaMutationResponse(
                message = "Deleted ${mutation.deletedPaths.size} media file(s).",
                selectedPath = null,
                affectedPaths = mutation.deletedPaths
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
            ready = true,
            repository = null,
            items = repository.count() + repository.mediaCount(),
            dirty = repository.dirtyCount() + repository.mediaDirtyCount(),
            lastSyncedAt = maxOfNullable(repository.lastSyncedAt(), repository.mediaLastSyncedAt()),
            git = gitSyncService.status()
        )
    }

    private fun refreshIndexLocked(): CmsRefreshResponse {
        val scannedEntries = contentFileService.scanAll()
        repository.replaceFromScan(scannedEntries)
        val scannedMedia = mediaFileService.scanAll()
        repository.replaceMediaFromScan(scannedMedia)
        previewInitialized = false
        return CmsRefreshResponse(
            items = repository.count() + repository.mediaCount(),
            dirty = repository.dirtyCount() + repository.mediaDirtyCount()
        )
    }

    private fun syncLocked(commitMessage: String?, push: Boolean?, accessToken: String?): CmsSyncResponse {
        val dirtyContentPaths = repository.dirtySourcePaths()
        val dirtyMediaPaths = repository.dirtyMediaSourcePaths()
        val dirtyPaths = dirtyContentPaths + dirtyMediaPaths
        val outcome = gitSyncService.sync(dirtyPaths, commitMessage, push, accessToken)

        if (outcome.committed) {
            val timestamp = System.currentTimeMillis()
            repository.markSynced(dirtyContentPaths, timestamp)
            repository.markMediaSynced(dirtyMediaPaths, timestamp)
        }

        return CmsSyncResponse(
            committed = outcome.committed,
            commitId = outcome.commitId,
            message = outcome.message,
            pushAttempted = outcome.pushAttempted,
            pushSucceeded = outcome.pushSucceeded,
            files = outcome.files,
            dirtyRemaining = repository.dirtyCount() + repository.mediaDirtyCount()
        )
    }

    private fun markMediaDeleted(sourcePath: String) {
        val existing = repository.findMedia(sourcePath)
        if (existing != null && existing.dirty && existing.lastSyncedAt == null) {
            repository.deleteMedia(sourcePath)
            return
        }

        repository.upsertMedia(
            CmsMediaEntry(
                sourcePath = sourcePath,
                rootPath = existing?.rootPath ?: mediaRootForPath(sourcePath),
                fileName = existing?.fileName ?: sourcePath.substringAfterLast('/'),
                size = 0,
                contentType = existing?.contentType,
                dirty = true,
                deleted = true,
                updatedAt = System.currentTimeMillis(),
                lastSyncedAt = existing?.lastSyncedAt
            )
        )
    }

    private fun markContentDeleted(sourcePath: String, lastSyncedAt: Long?, dirty: Boolean) {
        if (dirty && lastSyncedAt == null) {
            repository.deleteContent(sourcePath)
            return
        }

        repository.deleteContent(sourcePath)
        repository.markContentDeleted(sourcePath)
    }

    private fun deleteGeneratedContent(outputPath: String) {
        deleteGeneratedPath(rootPath.resolve(config.theme.output).normalize(), outputPath)
    }

    private fun deleteGeneratedPreviewContent(outputPath: String) {
        deleteGeneratedPath(previewOutputRoot(), outputPath)
    }

    private fun deleteGeneratedPath(outputRoot: Path, outputPath: String) {
        val target = if (outputPath.isBlank()) {
            outputRoot.resolve("index.html")
        } else {
            outputRoot.resolve(outputPath)
        }.normalize()

        if (!target.startsWith(outputRoot) || !Files.exists(target)) {
            return
        }

        if (Files.isDirectory(target)) {
            Files.walk(target).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { candidate ->
                    Files.deleteIfExists(candidate)
                }
            }
            pruneEmptyGeneratedDirectories(target.parent, outputRoot)
        } else {
            Files.deleteIfExists(target)
            pruneEmptyGeneratedDirectories(target.parent, outputRoot)
        }
    }

    private fun ensurePreviewInitializedLocked() {
        if (previewInitialized) {
            return
        }

        val outputRoot = previewOutputRoot()
        if (Files.exists(outputRoot)) {
            Files.walk(outputRoot).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { candidate ->
                    Files.deleteIfExists(candidate)
                }
            }
        }

        previewGenerator.generate()
        previewInitialized = true
    }

    private fun previewOutputRoot(): Path {
        return rootPath.resolve(previewConfig.theme.output).normalize()
    }

    private fun pruneEmptyGeneratedDirectories(start: Path?, stopAt: Path) {
        var current = start
        while (current != null && current.startsWith(stopAt) && current != stopAt) {
            val isEmpty = Files.list(current).use { stream -> !stream.findFirst().isPresent }
            if (!isEmpty) {
                return
            }
            Files.deleteIfExists(current)
            current = current.parent
        }
    }

    private fun composeMediaPath(targetDirectory: String, fileName: String): String {
        val directory = targetDirectory.trim().trimEnd('/').removePrefix("/")
        return listOf(directory, fileName.trim().removePrefix("/"))
            .filter { it.isNotBlank() }
            .joinToString("/")
            .replace('\\', '/')
    }

    private fun mediaRootForPath(sourcePath: String): String {
        val normalized = sourcePath.trim().removePrefix("/").replace('\\', '/')
        return config.theme.assets
            .sortedByDescending { it.length }
            .firstOrNull { normalized == it || normalized.startsWith("$it/") }
            ?: throw IllegalArgumentException("Unsupported media path: $sourcePath")
    }

    companion object {
        private fun contentOrdering(): Comparator<CmsContentEntry> {
            return Comparator { left, right ->
                when {
                    left.type != right.type -> left.type.name.compareTo(right.type.name)
                    left.type == CmsContentType.POST -> comparePosts(left, right)
                    else -> comparePages(left, right)
                }
            }
        }

        private fun comparePosts(left: CmsContentEntry, right: CmsContentEntry): Int {
            val leftDraft = left.metadata["draft"]?.toString()?.lowercase() in setOf("true", "yes", "1")
            val rightDraft = right.metadata["draft"]?.toString()?.lowercase() in setOf("true", "yes", "1")
            if (leftDraft != rightDraft) {
                return leftDraft.compareTo(rightDraft)
            }

            val leftDate = left.publishedAt.parseCmsDateOrNull() ?: LocalDateTime.MIN
            val rightDate = right.publishedAt.parseCmsDateOrNull() ?: LocalDateTime.MIN
            val byDate = rightDate.compareTo(leftDate)
            if (byDate != 0) {
                return byDate
            }

            return compareByTitleThenPath(left, right)
        }

        private fun comparePages(left: CmsContentEntry, right: CmsContentEntry): Int {
            val leftNav = left.metadata["nav_order"]?.toString()?.toIntOrNull()
                ?: left.metadata["navOrder"]?.toString()?.toIntOrNull()
                ?: Int.MAX_VALUE
            val rightNav = right.metadata["nav_order"]?.toString()?.toIntOrNull()
                ?: right.metadata["navOrder"]?.toString()?.toIntOrNull()
                ?: Int.MAX_VALUE
            val byNav = leftNav.compareTo(rightNav)
            if (byNav != 0) {
                return byNav
            }

            return compareByTitleThenPath(left, right)
        }

        private fun compareByTitleThenPath(left: CmsContentEntry, right: CmsContentEntry): Int {
            val byTitle = left.title.lowercase().compareTo(right.title.lowercase())
            if (byTitle != 0) {
                return byTitle
            }
            return left.sourcePath.compareTo(right.sourcePath)
        }

        private fun isDraft(metadata: Map<String, Any?>): Boolean {
            return metadata["draft"]?.toString()?.lowercase() in setOf("true", "yes", "1")
        }

        private fun String?.parseCmsDateOrNull(): LocalDateTime? {
            return runCatching { this?.let(LocalDateTime::parse) }.getOrNull()
        }

        private fun resolveDatabasePath(rootPath: Path, configuredPath: String): Path {
            val path = Paths.get(configuredPath)
            return if (path.isAbsolute) path else rootPath.resolve(path).normalize()
        }

        private fun resolveGeneratedFile(outputRoot: Path, requestedPath: String): Path? {
            val normalizedRoot = outputRoot.toAbsolutePath().normalize()
            if (!Files.exists(normalizedRoot)) {
                return null
            }

            val relativePath = requestedPath.trim().removePrefix("/").removeSuffix("/")
            val candidates = if (relativePath.isBlank()) {
                listOf(normalizedRoot.resolve("index.html"))
            } else {
                buildList {
                    val direct = normalizedRoot.resolve(relativePath).normalize()
                    add(direct)
                    if (!relativePath.substringAfterLast('/', "").contains('.')) {
                        add(direct.resolve("index.html").normalize())
                    }
                }
            }

            return candidates.firstOrNull { candidate ->
                candidate.startsWith(normalizedRoot) && Files.isRegularFile(candidate)
            }
        }

        private fun maxOfNullable(left: Long?, right: Long?): Long? {
            return when {
                left == null -> right
                right == null -> left
                else -> maxOf(left, right)
            }
        }

        internal fun normalizeBasePath(basePath: String): String {
            val trimmed = basePath.trim().ifBlank { "/__statik__/cms" }
            return "/" + trimmed.removePrefix("/").removeSuffix("/")
        }

        private fun normalizeSourcePath(sourcePath: String): String {
            return sourcePath.trim().removePrefix("/").replace('\\', '/')
        }

        private const val PREVIEW_OUTPUT_DIRECTORY = ".statik/cms-preview"
    }
}
