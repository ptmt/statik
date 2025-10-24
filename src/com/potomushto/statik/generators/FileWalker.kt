package com.potomushto.statik.generators

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.streams.asSequence

class FileWalker(
    private val rootPath: String,
    private val excludedFileNames: Set<String> = setOf("index")
) {
    private val contentExtensions = setOf("md", "html", "hbs")

    /**
     * Walk through a content directory (posts, pages, etc) and return content files (.md, .html, .hbs)
     * @param excludeIndex if true, exclude files whose name (without extension) is in excludedFileNames
     */
    fun walkMarkdownFiles(contentPath: String, excludeIndex: Boolean = false): Sequence<Path> {
        val absolutePath = Paths.get(rootPath, contentPath)
        if (!Files.exists(absolutePath)) {
            return emptySequence()
        }

        return Files.walk(absolutePath)
            .asSequence()
            .filter { Files.isRegularFile(it) }
            .filter { it.extension in contentExtensions }
            .filter { file ->
                if (excludeIndex) {
                    val fileNameWithoutExtension = file.fileName.toString().substringBeforeLast('.')
                    fileNameWithoutExtension !in excludedFileNames
                } else {
                    true
                }
            }
    }

    /**
     * Walks through the static files directory and returns a sequence of files
     */
    fun walkStaticFiles(assetsPath: String): Sequence<Path> {
        val staticPath = Paths.get(rootPath, assetsPath)
        if (!Files.exists(staticPath)) {
            return emptySequence()
        }

        return Files.walk(staticPath)
            .asSequence()
            .filter { Files.isRegularFile(it) }
    }

    /**
     * Generates a URL path relative to the content root based on the file path
     */
    fun generatePath(file: Path, contentPath: String, stripIndex: Boolean = false): String {
        val contentRoot = Paths.get(rootPath, contentPath)
        val relativePath = contentRoot.relativize(file)

        val pathWithoutExtension = relativePath.toString()
            .replace('\\', '/')
            .substringBeforeLast('.')

        if (!stripIndex) {
            return pathWithoutExtension
        }

        return when {
            pathWithoutExtension == "index" -> ""
            pathWithoutExtension.endsWith("/index") -> pathWithoutExtension.removeSuffix("/index")
            else -> pathWithoutExtension
        }
    }
}
