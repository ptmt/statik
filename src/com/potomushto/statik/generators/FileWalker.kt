package com.potomushto.statik.generators

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.streams.asSequence

class FileWalker(private val rootPath: String) {
    
    /**
     * Walks through the blog files directory and returns a sequence of markdown files
     */
    fun walkBlogFiles(): Sequence<Path> {
        val postsPath = Paths.get(rootPath, "posts")
        return Files.walk(postsPath)
            .asSequence()
            .filter { Files.isRegularFile(it) }
            .filter { it.extension in setOf("md", "html") }
    }

    /**
     * Walks through the static files directory and returns a sequence of files
     */
    fun walkStaticFiles(assetsPath: String): Sequence<Path> {
        val staticPath = Paths.get(assetsPath)
        return Files.walk(staticPath)
            .asSequence()
            .filter { Files.isRegularFile(it) }
    }

    /**
     * Generates a URL path for a blog post based on its file path
     */
    fun generatePath(file: Path): String {
        val postsPath = Paths.get(rootPath, "content", "posts")
        val relativePath = postsPath.relativize(file)
        
        // Remove the file extension and convert to URL path
        return relativePath.toString()
            .replace('\\', '/')
            .substringBeforeLast('.')
    }
} 