package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.Charsets.UTF_8
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
    private val outputPath = Paths.get(rootPath, config.theme.output).toAbsolutePath().normalize()

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

        // Assets are needed before posts so output sizes can include local images.
        assetManager.copyAllAssets()

        // Generate posts first so listing templates can use generated output sizes.
        val contextWithOutputSizes = buildAllPosts(context)

        // Generate all pages
        buildHomePage(contextWithOutputSizes)
        buildPostsPage(contextWithOutputSizes)
        buildAllPages(contextWithOutputSizes)

        // Generate supplementary content
        rssGenerator.generate(contextWithOutputSizes.posts)
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

        // Asset sizes can contribute to post output sizes, so copy them before rebuilding listings.
        changes.assetFiles.forEach { assetFile ->
            logger.info { "Copying asset: ${assetFile.fileName}" }
            assetManager.copySingleAsset(assetFile)
        }

        // Handle post changes
        changes.postFiles.forEach { postFile ->
            val postId = postFile.nameWithoutExtension
            logger.info { "Rebuilding post: $postId" }
            contentRepository.invalidatePost(postId)

            // Reload posts after invalidation
            val updatedPosts = contentRepository.loadAllPosts()
            val updatedContext = context.copy(posts = updatedPosts)

            buildSinglePost(postId, updatedContext)
            val updatedContextWithOutputSizes = attachPostOutputSizes(updatedContext)

            // Home page shows post list, so rebuild it too
            buildHomePage(updatedContextWithOutputSizes)

            // RSS feed includes posts
            rssGenerator.generate(updatedContextWithOutputSizes.posts)

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

        if (changes.assetFiles.isNotEmpty() && changes.postFiles.isEmpty()) {
            val contextWithOutputSizes = attachPostOutputSizes(context)
            buildHomePage(contextWithOutputSizes)
            buildPostsPage(contextWithOutputSizes)
        }

        logger.info { "Incremental build completed" }
    }

    /**
     * Build a single blog post
     */
    fun buildSinglePost(postId: String, context: BuildContext): Long? {
        val post = context.posts.find { it.id == postId }
        if (post == null) {
            logger.warn { "Post not found: $postId" }
            return null
        }

        val html = templateRenderer.renderPost(post, context.pages, context.datasourceContext)
        val finalHtml = injectLiveReloadIfNeeded(html)
        val outputFile = outputPath.resolve(post.path).resolve("index.html")
        outputFile.parent.createDirectories()
        Files.writeString(outputFile, finalHtml)

        logger.debug { "Built post: $postId -> ${post.path}" }
        return renderedPageSizeBytes(finalHtml, post.path)
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
    private fun buildAllPosts(context: BuildContext): BuildContext {
        val postsWithOutputSizes = context.posts.map { post ->
            val outputSizeBytes = buildSinglePost(post.id, context)
            post.copy(outputSizeBytes = outputSizeBytes)
        }
        return context.copy(posts = postsWithOutputSizes)
    }

    private fun attachPostOutputSizes(context: BuildContext): BuildContext {
        val postsWithOutputSizes = context.posts.map { post ->
            val outputFile = outputPath.resolve(post.path).resolve("index.html")
            val outputSizeBytes = if (Files.exists(outputFile)) {
                renderedPageSizeBytes(Files.readString(outputFile), post.path)
            } else {
                post.outputSizeBytes
            }
            post.copy(outputSizeBytes = outputSizeBytes)
        }
        return context.copy(posts = postsWithOutputSizes)
    }

    private fun renderedPageSizeBytes(html: String, pagePath: String): Long {
        val htmlBytes = html.toByteArray(UTF_8).size.toLong()
        val imageBytes = Jsoup.parse(html)
            .select("img[src]")
            .mapNotNull { resolveLocalOutputAsset(it.attr("src"), pagePath) }
            .toSet()
            .sumOf { Files.size(it) }
        return htmlBytes + imageBytes
    }

    private fun resolveLocalOutputAsset(src: String, pagePath: String): Path? {
        val normalizedSrc = src.trim()
        if (
            normalizedSrc.isEmpty() ||
            normalizedSrc.startsWith("http://", ignoreCase = true) ||
            normalizedSrc.startsWith("https://", ignoreCase = true) ||
            normalizedSrc.startsWith("//") ||
            normalizedSrc.startsWith("data:", ignoreCase = true)
        ) {
            return null
        }

        val srcPath = normalizedSrc
            .substringBefore('#')
            .substringBefore('?')
            .replace('\\', '/')
            .takeIf { it.isNotBlank() }
            ?: return null

        val outputRelativePath = if (srcPath.startsWith("/")) {
            srcPath.removePrefix("/")
        } else {
            val pageDirectory = pagePath.trim('/').substringBeforeLast("/", missingDelimiterValue = "")
            if (pageDirectory.isBlank()) srcPath else "$pageDirectory/$srcPath"
        }

        val decodedPath = try {
            URLDecoder.decode(outputRelativePath, UTF_8.name())
        } catch (_: IllegalArgumentException) {
            outputRelativePath
        }

        val outputAsset = outputPath.resolve(decodedPath).normalize()
        return outputAsset.takeIf { it.startsWith(outputPath) && Files.isRegularFile(it) }
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
        val pageDirs = config.paths.pages.map { rootDir.resolve(it) }
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
                pageDirs.any { file.startsWith(it) } && isContentFile(file) -> pageFiles.add(file)
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
