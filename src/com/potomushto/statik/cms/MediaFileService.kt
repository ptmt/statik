package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.FileWalker
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.Comparator

class MediaFileService(
    private val rootPath: Path,
    private val config: BlogConfig,
    private val fileWalker: FileWalker = FileWalker(rootPath.toString())
) {
    private val assetRoots = config.theme.assets.sortedByDescending { it.length }

    fun managedRoots(): List<String> = config.theme.assets

    fun scanAll(): List<CmsMediaEntry> {
        return config.theme.assets.flatMap { assetRoot ->
            fileWalker.walkStaticFiles(assetRoot)
                .map { scanFile(it, assetRoot) }
                .toList()
        }.sortedBy { it.sourcePath }
    }

    fun upload(request: CmsMediaUploadRequest): CmsMediaEntry {
        val targetDirectory = request.targetDirectory.trim().ifBlank {
            config.theme.assets.firstOrNull() ?: throw IllegalStateException("No asset directories configured")
        }
        require(request.fileName.isNotBlank()) { "Missing media file name" }
        require(!request.fileName.contains('/')) { "Media file name must not contain '/'" }
        require(!request.fileName.contains('\\')) { "Media file name must not contain '\\'" }

        val location = resolveLocation(targetDirectory, allowRootDirectory = true)
        require(!Files.exists(location.absolutePath) || Files.isDirectory(location.absolutePath)) {
            "Media upload target is not a directory: ${location.sourcePath}"
        }

        val target = location.absolutePath.resolve(request.fileName).normalize()
        require(target.startsWith(rootPath.resolve(location.rootPath).normalize())) {
            "Media upload target escapes asset root: ${location.sourcePath}"
        }

        Files.createDirectories(target.parent)
        Files.write(target, Base64.getDecoder().decode(request.contentsBase64))
        return scanFile(target, location.rootPath)
    }

    fun rename(sourcePath: String, targetPath: String): MediaMutation {
        val source = resolveLocation(sourcePath, allowRootDirectory = false)
        val target = resolveLocation(targetPath, allowRootDirectory = false)
        require(source.sourcePath != target.sourcePath) { "Media target path matches source path" }
        require(Files.exists(source.absolutePath)) { "Media path does not exist: ${source.sourcePath}" }
        require(!Files.exists(target.absolutePath)) { "Media target already exists: ${target.sourcePath}" }

        return if (Files.isDirectory(source.absolutePath)) {
            renameDirectory(source, target)
        } else {
            renameFile(source, target)
        }
    }

    fun delete(sourcePath: String): MediaMutation {
        val source = resolveLocation(sourcePath, allowRootDirectory = false)
        require(Files.exists(source.absolutePath)) { "Media path does not exist: ${source.sourcePath}" }

        return if (Files.isDirectory(source.absolutePath)) {
            deleteDirectory(source)
        } else {
            deleteFile(source)
        }
    }

    private fun renameFile(source: AssetLocation, target: AssetLocation): MediaMutation {
        Files.createDirectories(target.absolutePath.parent)
        Files.move(source.absolutePath, target.absolutePath)
        cleanupEmptyParents(source.absolutePath.parent, rootPath.resolve(source.rootPath).normalize())

        return MediaMutation(
            updatedEntries = listOf(scanFile(target.absolutePath, target.rootPath)),
            deletedPaths = listOf(source.sourcePath)
        )
    }

    private fun renameDirectory(source: AssetLocation, target: AssetLocation): MediaMutation {
        require(!target.absolutePath.startsWith(source.absolutePath)) {
            "Cannot move a media directory into itself: ${source.sourcePath}"
        }

        val sourceFiles = walkFiles(source.absolutePath)
        sourceFiles.forEach { sourceFile ->
            val relativePath = source.absolutePath.relativize(sourceFile)
            val targetFile = target.absolutePath.resolve(relativePath).normalize()
            Files.createDirectories(targetFile.parent)
            Files.move(sourceFile, targetFile)
        }
        deleteRecursivelyIfExists(source.absolutePath)
        cleanupEmptyParents(source.absolutePath.parent, rootPath.resolve(source.rootPath).normalize())

        val movedEntries = walkFiles(target.absolutePath)
            .map { scanFile(it, target.rootPath) }

        return MediaMutation(
            updatedEntries = movedEntries,
            deletedPaths = sourceFiles.map { toSourcePath(it) }
        )
    }

    private fun deleteFile(source: AssetLocation): MediaMutation {
        Files.delete(source.absolutePath)
        cleanupEmptyParents(source.absolutePath.parent, rootPath.resolve(source.rootPath).normalize())
        return MediaMutation(
            updatedEntries = emptyList(),
            deletedPaths = listOf(source.sourcePath)
        )
    }

    private fun deleteDirectory(source: AssetLocation): MediaMutation {
        val deletedPaths = walkFiles(source.absolutePath).map { toSourcePath(it) }
        deleteRecursivelyIfExists(source.absolutePath)
        cleanupEmptyParents(source.absolutePath.parent, rootPath.resolve(source.rootPath).normalize())
        return MediaMutation(
            updatedEntries = emptyList(),
            deletedPaths = deletedPaths
        )
    }

    private fun scanFile(file: Path, assetRoot: String): CmsMediaEntry {
        val sourcePath = toSourcePath(file)
        return CmsMediaEntry(
            sourcePath = sourcePath,
            rootPath = assetRoot,
            fileName = file.fileName.toString(),
            size = Files.size(file),
            contentType = Files.probeContentType(file),
            dirty = false,
            deleted = false,
            updatedAt = Files.getLastModifiedTime(file).toMillis(),
            lastSyncedAt = null
        )
    }

    private fun walkFiles(directory: Path): List<Path> {
        if (!Files.exists(directory)) {
            return emptyList()
        }

        Files.walk(directory).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) }
                .sorted()
                .toList()
        }
    }

    private fun deleteRecursivelyIfExists(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { candidate ->
                Files.deleteIfExists(candidate)
            }
        }
    }

    private fun cleanupEmptyParents(start: Path?, stopAt: Path) {
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

    private fun resolveLocation(path: String, allowRootDirectory: Boolean): AssetLocation {
        val normalizedPath = path.trim().removePrefix("/").replace('\\', '/')
        require(normalizedPath.isNotBlank()) { "Missing media path" }

        val assetRoot = assetRoots.firstOrNull { normalizedPath == it || normalizedPath.startsWith("$it/") }
            ?: throw IllegalArgumentException("Unsupported media path: $path")

        if (!allowRootDirectory && normalizedPath == assetRoot) {
            throw IllegalArgumentException("Cannot modify media root: $assetRoot")
        }

        val absolutePath = rootPath.resolve(normalizedPath).normalize()
        require(absolutePath.startsWith(rootPath.normalize())) {
            "Media path escapes the site root: $path"
        }

        return AssetLocation(
            sourcePath = normalizedPath,
            rootPath = assetRoot,
            absolutePath = absolutePath
        )
    }

    private fun toSourcePath(path: Path): String {
        return rootPath.relativize(path).toString().replace('\\', '/')
    }

    private data class AssetLocation(
        val sourcePath: String,
        val rootPath: String,
        val absolutePath: Path
    )
}

data class MediaMutation(
    val updatedEntries: List<CmsMediaEntry>,
    val deletedPaths: List<String>
)
