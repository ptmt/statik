package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.FileWalker
import com.potomushto.statik.processors.FrontmatterParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class ContentFileService(
    private val rootPath: Path,
    private val config: BlogConfig,
    private val fileWalker: FileWalker = FileWalker(rootPath.toString())
) {
    private val supportedExtensions = setOf("md", "html", "hbs")

    fun scanAll(): List<CmsContentEntry> {
        val posts = fileWalker.walkMarkdownFiles(config.paths.posts, excludeIndex = true)
            .map { scanFile(it, CmsContentType.POST, config.paths.posts) }
            .toList()

        val pages = config.paths.pages.flatMap { pagesRoot ->
            fileWalker.walkMarkdownFiles(pagesRoot)
                .map { scanFile(it, CmsContentType.PAGE, pagesRoot) }
                .toList()
        }

        return (posts + pages)
            .sortedWith(
                compareBy<CmsContentEntry> { it.type.name }
                    .thenBy { it.sourcePath }
            )
    }

    fun read(sourcePath: String): CmsContentEntry? {
        val location = resolveLocation(sourcePath, null) ?: return null
        val absolutePath = rootPath.resolve(location.sourcePath).normalize()
        if (!Files.exists(absolutePath)) {
            return null
        }
        return scanFile(absolutePath, location.type, location.contentRoot)
    }

    fun save(request: CmsSaveRequest): CmsContentEntry {
        val location = resolveLocation(request.sourcePath, request.type)
            ?: throw IllegalArgumentException("Unsupported content path: ${request.sourcePath}")

        val absolutePath = rootPath.resolve(location.sourcePath).normalize()
        absolutePath.parent?.let { Files.createDirectories(it) }

        val serialized = FrontmatterParser.serialize(request.frontmatter, request.body)
        Files.writeString(absolutePath, serialized)

        return scanFile(absolutePath, location.type, location.contentRoot)
    }

    fun rename(sourcePath: String, targetPath: String, expectedType: CmsContentType): CmsContentEntry {
        val source = resolveLocation(sourcePath, expectedType)
            ?: throw IllegalArgumentException("Unsupported content path: $sourcePath")
        val target = resolveLocation(targetPath, expectedType)
            ?: throw IllegalArgumentException("Unsupported content path: $targetPath")

        require(source.sourcePath != target.sourcePath) { "Content target path matches source path" }

        val sourceAbsolutePath = rootPath.resolve(source.sourcePath).normalize()
        val targetAbsolutePath = rootPath.resolve(target.sourcePath).normalize()
        require(Files.exists(sourceAbsolutePath)) { "Content path does not exist: ${source.sourcePath}" }
        require(!Files.exists(targetAbsolutePath)) { "Content target already exists: ${target.sourcePath}" }

        targetAbsolutePath.parent?.let { Files.createDirectories(it) }
        Files.move(sourceAbsolutePath, targetAbsolutePath, StandardCopyOption.ATOMIC_MOVE)
        cleanupEmptyParents(sourceAbsolutePath.parent, rootPath.resolve(source.contentRoot).normalize())

        return scanFile(targetAbsolutePath, target.type, target.contentRoot)
    }

    private fun scanFile(file: Path, type: CmsContentType, contentRoot: String): CmsContentEntry {
        val raw = Files.readString(file)
        val document = FrontmatterParser.extractDocument(raw)
        val sourcePath = rootPath.relativize(file).toString().replace('\\', '/')
        val title = document.metadata["title"]?.toString()?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
        val outputPath = when (type) {
            CmsContentType.POST -> fileWalker.generatePath(file, contentRoot)
            CmsContentType.PAGE -> resolvePageOutputPath(file, contentRoot)
        }

        return CmsContentEntry(
            type = type,
            sourcePath = sourcePath,
            outputPath = outputPath,
            title = title,
            frontmatter = document.frontmatter,
            body = document.body,
            extension = file.extension.lowercase(),
            metadata = document.metadata,
            publishedAt = document.metadata["published"]?.toString()?.takeIf { it.isNotBlank() },
            dirty = false,
            updatedAt = Files.getLastModifiedTime(file).toMillis(),
            lastSyncedAt = null
        )
    }

    private fun resolvePageOutputPath(file: Path, pagesDirectory: String): String {
        val basePath = fileWalker.generatePath(file, pagesDirectory, stripIndex = true)
        return if (basePath.isEmpty() && pagesDirectory != "pages") {
            pagesDirectory
        } else if (pagesDirectory != "pages" && !basePath.startsWith("$pagesDirectory/")) {
            "$pagesDirectory/$basePath"
        } else {
            basePath
        }
    }

    private fun resolveLocation(sourcePath: String, expectedType: CmsContentType?): ContentLocation? {
        val normalizedPath = sourcePath.trim().removePrefix("/").replace('\\', '/')
        if (normalizedPath.isBlank()) {
            return null
        }

        val extension = Paths.get(normalizedPath).extension.lowercase()
        if (extension !in supportedExtensions) {
            throw IllegalArgumentException("Unsupported content extension: .$extension")
        }

        val absolutePath = rootPath.resolve(normalizedPath).normalize()
        require(absolutePath.startsWith(rootPath.normalize())) {
            "Content path escapes the site root: $sourcePath"
        }

        if (matchesRoot(normalizedPath, config.paths.posts)) {
            if (expectedType != null && expectedType != CmsContentType.POST) {
                throw IllegalArgumentException("Content path '$sourcePath' must be saved as POST")
            }
            return ContentLocation(CmsContentType.POST, config.paths.posts, normalizedPath)
        }

        val matchingPagesRoot = config.paths.pages
            .sortedByDescending { it.length }
            .firstOrNull { matchesRoot(normalizedPath, it) }
            ?: return null

        if (expectedType != null && expectedType != CmsContentType.PAGE) {
            throw IllegalArgumentException("Content path '$sourcePath' must be saved as PAGE")
        }

        return ContentLocation(CmsContentType.PAGE, matchingPagesRoot, normalizedPath)
    }

    private fun matchesRoot(path: String, root: String): Boolean {
        return path == root || path.startsWith("$root/")
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

    private data class ContentLocation(
        val type: CmsContentType,
        val contentRoot: String,
        val sourcePath: String
    )
}
