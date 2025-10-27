package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import com.potomushto.statik.processors.ContentProcessor
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Responsible for loading and caching content (posts and pages) from the filesystem
 */
class ContentRepository(
    private val rootPath: String,
    private val config: BlogConfig,
    private val fileWalker: FileWalker,
    private val contentProcessor: ContentProcessor
) {
    private val logger = LoggerFactory.getLogger(ContentRepository::class.java)

    // Caches for watch mode builds
    private var postsCache: List<BlogPost>? = null
    private var pagesCache: List<SitePage>? = null

    /**
     * Load all blog posts, using cache if available
     */
    fun loadAllPosts(useCache: Boolean = true): List<BlogPost> {
        if (useCache && postsCache != null) {
            return postsCache!!
        }

        logger.debug { "Loading all posts from ${config.paths.posts}" }
        val posts = loadPostsFromDisk()
        postsCache = posts
        return posts
    }

    /**
     * Load all pages, using cache if available
     */
    fun loadAllPages(useCache: Boolean = true): List<SitePage> {
        if (useCache && pagesCache != null) {
            return pagesCache!!
        }

        logger.debug { "Loading all pages from ${config.paths.pages}" }
        val pages = loadPagesFromDisk()
        pagesCache = pages
        return pages
    }

    /**
     * Load a single post by ID, or reload from disk if not in cache
     */
    fun loadPostById(id: String, useCache: Boolean = true): BlogPost? {
        val cached = if (useCache) postsCache?.find { it.id == id } else null
        if (cached != null) return cached

        // Reload all posts if not in cache
        return loadAllPosts(useCache = false).find { it.id == id }
    }

    /**
     * Load a single page by ID, or reload from disk if not in cache
     */
    fun loadPageById(id: String, useCache: Boolean = true): SitePage? {
        val cached = if (useCache) pagesCache?.find { it.id == id } else null
        if (cached != null) return cached

        // Reload all pages if not in cache
        return loadAllPages(useCache = false).find { it.id == id }
    }

    /**
     * Invalidate a specific post in cache and reload it
     */
    fun invalidatePost(postId: String): BlogPost? {
        logger.debug { "Invalidating post cache for: $postId" }
        postsCache = null
        return loadPostById(postId, useCache = false)
    }

    /**
     * Invalidate a specific page in cache and reload it
     */
    fun invalidatePage(pageId: String): SitePage? {
        logger.debug { "Invalidating page cache for: $pageId" }
        pagesCache = null
        return loadPageById(pageId, useCache = false)
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        logger.debug { "Clearing all content caches" }
        postsCache = null
        pagesCache = null
    }

    private fun loadPostsFromDisk(): List<BlogPost> {
        val postsDirectory = config.paths.posts
        return fileWalker.walkMarkdownFiles(postsDirectory, excludeIndex = true)
            .map { file ->
                val parsedPost = contentProcessor.process(file)
                val title = parsedPost.metadata["title"] ?: file.nameWithoutExtension
                val date = parsedPost.metadata["published"]?.let { LocalDateTime.parse(it) }
                    ?: Files.getLastModifiedTime(file).let {
                        LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault())
                    }

                BlogPost(
                    id = file.nameWithoutExtension,
                    title = title,
                    date = date,
                    content = parsedPost.content,
                    rawHtml = null,
                    metadata = parsedPost.metadata,
                    outputPath = fileWalker.generatePath(file, postsDirectory),
                    isTemplate = file.extension.lowercase() == "hbs"
                )
            }
            .sortedByDescending { it.date }
            .toList()
    }

    private fun loadPagesFromDisk(): List<SitePage> {
        val pagesDirectory = config.paths.pages
        return fileWalker.walkMarkdownFiles(pagesDirectory)
            .map { file ->
                val parsedPage = contentProcessor.process(file)
                val title = parsedPage.metadata["title"] ?: file.nameWithoutExtension
                val navOrder = parsedPage.metadata["nav_order"]?.toIntOrNull()
                    ?: parsedPage.metadata["navOrder"]?.toIntOrNull()

                SitePage(
                    id = file.nameWithoutExtension,
                    title = title,
                    content = parsedPage.content,
                    metadata = parsedPage.metadata,
                    outputPath = fileWalker.generatePath(file, pagesDirectory, stripIndex = true),
                    navOrder = navOrder,
                    isTemplate = file.extension.lowercase() == "hbs"
                )
            }
            .sortedWith(compareBy<SitePage> { it.navOrder ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() })
            .toList()
    }
}
