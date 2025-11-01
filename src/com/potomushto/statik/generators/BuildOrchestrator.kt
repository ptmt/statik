package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Orchestrates the site build process, handling both full and incremental builds
 */
class BuildOrchestrator(
    private val rootPath: String,
    private val config: BlogConfig,
    private val contentRepository: ContentRepository,
    private val templateRenderer: TemplateRenderer,
    private val assetManager: AssetManager,
    private val rssGenerator: RssGenerator,
    private val datasourceGenerator: StaticDatasourceGenerator,
    private val injectLiveReload: Boolean = false
) {
    private val logger = LoggerFactory.getLogger(BuildOrchestrator::class.java)
    private val outputPath = Paths.get(rootPath, config.theme.output)

    private val liveReloadScript = """
        <script src="/__statik__/livereload.js"></script>
    """.trimIndent()

    /**
     * Perform a full site build
     */
    fun buildFull() {
        logger.debug { "Starting full site build" }

        // Load all content (will use cache if available)
        val posts = contentRepository.loadAllPosts()
        val pages = contentRepository.loadAllPages()

        // Build datasource context
        val datasourceBundle = datasourceGenerator.buildBundle(posts, pages)
        val datasourceContext = datasourceBundle.toTemplateContext()

        val context = BuildContext(posts, pages, datasourceContext)

        // Generate all pages
        buildHomePage(context)
        buildPostsPage(context)
        buildAllPosts(context)
        buildAllPages(context)

        // Generate supplementary content
        rssGenerator.generate(posts)
        assetManager.copyAllAssets()
        datasourceGenerator.writeBundle(datasourceBundle)

        logger.debug { "Full site build completed" }
    }

    /**
     * Perform an incremental build based on changed files
     */
    fun buildIncremental(changedFiles: List<Path>) {
        if (changedFiles.isEmpty()) {
            logger.debug { "No files changed, skipping build" }
            return
        }

        logger.info { "Starting incremental build for ${changedFiles.size} changed file(s)" }

        // Classify changes
        val changes = classifyChanges(changedFiles)

        // If config changed, do full rebuild
        if (changes.configChanged) {
            logger.info { "Config changed, performing full rebuild" }
            contentRepository.clearCache()
            buildFull()
            return
        }

        // If templates changed, need to rebuild everything that uses them
        if (changes.templateFiles.isNotEmpty()) {
            logger.info { "Templates changed, performing full rebuild" }
            buildFull()
            return
        }

        // Load current context
        val posts = contentRepository.loadAllPosts()
        val pages = contentRepository.loadAllPages()
        val datasourceBundle = datasourceGenerator.buildBundle(posts, pages)
        val datasourceContext = datasourceBundle.toTemplateContext()
        val context = BuildContext(posts, pages, datasourceContext)

        // Handle post changes
        changes.postFiles.forEach { postFile ->
            val postId = postFile.nameWithoutExtension
            logger.info { "Rebuilding post: $postId" }
            contentRepository.invalidatePost(postId)

            // Reload posts after invalidation
            val updatedPosts = contentRepository.loadAllPosts()
            val updatedContext = context.copy(posts = updatedPosts)

            buildSinglePost(postId, updatedContext)

            // Home page shows post list, so rebuild it too
            buildHomePage(updatedContext)

            // RSS feed includes posts
            rssGenerator.generate(updatedPosts)

            // Update datasource
            val updatedBundle = datasourceGenerator.buildBundle(updatedPosts, pages)
            datasourceGenerator.writeBundle(updatedBundle)
        }

        // Handle page changes
        changes.pageFiles.forEach { pageFile ->
            val pageId = pageFile.nameWithoutExtension
            logger.info { "Rebuilding page: $pageId" }
            contentRepository.invalidatePage(pageId)

            // Reload pages after invalidation
            val updatedPages = contentRepository.loadAllPages()
            val updatedContext = context.copy(pages = updatedPages)

            buildSinglePage(pageId, updatedContext)

            // Update datasource
            val updatedBundle = datasourceGenerator.buildBundle(posts, updatedPages)
            datasourceGenerator.writeBundle(updatedBundle)
        }

        // Handle asset changes
        changes.assetFiles.forEach { assetFile ->
            logger.info { "Copying asset: ${assetFile.fileName}" }
            assetManager.copySingleAsset(assetFile)
        }

        logger.info { "Incremental build completed" }
    }

    /**
     * Build a single blog post
     */
    fun buildSinglePost(postId: String, context: BuildContext) {
        val post = context.posts.find { it.id == postId }
        if (post == null) {
            logger.warn { "Post not found: $postId" }
            return
        }

        val html = templateRenderer.renderPost(post, context.pages, context.datasourceContext)
        val finalHtml = injectLiveReloadIfNeeded(html)
        val outputFile = outputPath.resolve(post.path).resolve("index.html")
        outputFile.parent.createDirectories()
        Files.writeString(outputFile, finalHtml)

        logger.debug { "Built post: $postId -> ${post.path}" }
    }

    /**
     * Build a single page
     */
    fun buildSinglePage(pageId: String, context: BuildContext) {
        val page = context.pages.find { it.id == pageId }
        if (page == null) {
            logger.warn { "Page not found: $pageId" }
            return
        }

        val html = templateRenderer.renderPage(page, context.pages, context.datasourceContext)
        val finalHtml = injectLiveReloadIfNeeded(html)

        val pageOutputDir = if (page.path.isNotEmpty()) {
            outputPath.resolve(page.path)
        } else {
            outputPath
        }

        val outputFile = pageOutputDir.resolve("index.html")
        outputFile.parent.createDirectories()
        Files.writeString(outputFile, finalHtml)

        logger.debug { "Built page: $pageId -> ${page.path}" }
    }

    /**
     * Build the home page
     */
    fun buildHomePage(context: BuildContext) {
        val html = templateRenderer.renderHomePage(context.posts, context.pages, context.datasourceContext)
        val finalHtml = injectLiveReloadIfNeeded(html)
        val outputFile = outputPath.resolve("index.html")
        outputFile.parent.createDirectories()
        Files.writeString(outputFile, finalHtml)

        logger.debug { "Built home page" }
    }

    /**
     * Build the posts listing page
     */
    fun buildPostsPage(context: BuildContext) {
        val html = templateRenderer.renderPostsPage(context.posts, context.pages, context.datasourceContext)
        val finalHtml = injectLiveReloadIfNeeded(html)
        val outputFile = outputPath.resolve("posts").resolve("index.html")
        outputFile.parent.createDirectories()
        Files.writeString(outputFile, finalHtml)

        logger.debug { "Built posts page" }
    }

    /**
     * Build all blog posts
     */
    private fun buildAllPosts(context: BuildContext) {
        context.posts.forEach { post ->
            buildSinglePost(post.id, context)
        }
    }

    /**
     * Build all pages
     */
    private fun buildAllPages(context: BuildContext) {
        context.pages.forEach { page ->
            buildSinglePage(page.id, context)
        }
    }

    /**
     * Classify changed files into categories
     */
    private fun classifyChanges(changedFiles: List<Path>): FileChanges {
        val rootDir = Paths.get(rootPath)
        val postsDir = rootDir.resolve(config.paths.posts)
        val pagesDir = rootDir.resolve(config.paths.pages)
        val templatesDir = rootDir.resolve(config.theme.templates)
        val assetDirs = config.theme.assets.map { rootDir.resolve(it) }

        val postFiles = mutableListOf<Path>()
        val pageFiles = mutableListOf<Path>()
        val templateFiles = mutableListOf<Path>()
        val assetFiles = mutableListOf<Path>()
        var configChanged = false

        changedFiles.forEach { file ->
            when {
                file.fileName.toString() == "config.json" -> configChanged = true
                file.startsWith(postsDir) && isContentFile(file) -> postFiles.add(file)
                file.startsWith(pagesDir) && isContentFile(file) -> pageFiles.add(file)
                file.startsWith(templatesDir) -> templateFiles.add(file)
                assetDirs.any { file.startsWith(it) } -> assetFiles.add(file)
                else -> logger.debug { "Unclassified change: $file" }
            }
        }

        return FileChanges(
            postFiles = postFiles,
            pageFiles = pageFiles,
            templateFiles = templateFiles,
            assetFiles = assetFiles,
            configChanged = configChanged
        )
    }

    private fun isContentFile(file: Path): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("md", "html", "hbs")
    }

    /**
     * Inject live reload script into HTML if in development mode
     */
    private fun injectLiveReloadIfNeeded(html: String): String {
        if (!injectLiveReload) return html

        // Inject before closing </body> tag, or at the end if no body tag
        return if (html.contains("</body>", ignoreCase = true)) {
            html.replace(Regex("</body>", RegexOption.IGNORE_CASE), "$liveReloadScript\n</body>")
        } else {
            html + "\n$liveReloadScript"
        }
    }

    private data class FileChanges(
        val postFiles: List<Path>,
        val pageFiles: List<Path>,
        val templateFiles: List<Path>,
        val assetFiles: List<Path>,
        val configChanged: Boolean
    )
}
