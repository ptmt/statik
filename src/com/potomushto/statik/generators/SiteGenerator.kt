package com.potomushto.statik.generators

import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import kotlin.io.path.readText
import kotlin.io.path.extension
import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.processors.MarkdownProcessor
import com.potomushto.statik.processors.ContentProcessor
import com.potomushto.statik.template.HandlebarsTemplateEngine
import com.potomushto.statik.template.FallbackTemplates
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension

class SiteGenerator(private val rootPath: String,
                    private val config: BlogConfig,
                    private val baseUrlOverride: String? = null) {
    private val templatesPath = Paths.get(rootPath, config.theme.templates)
    private val markdownProcessor = MarkdownProcessor()
    private val contentProcessor = ContentProcessor(markdownProcessor)
    private val templateEngine = HandlebarsTemplateEngine(templatesPath)
    private val fileWalker = FileWalker(rootPath)
    private val datasourceGenerator = StaticDatasourceGenerator(
        Paths.get(rootPath),
        Paths.get(rootPath, config.theme.output),
        config.staticDatasource,
        contentProcessor
    )
    private val rssGenerator = RssGenerator(
        config,
        Paths.get(rootPath, config.theme.output),
        baseUrlOverride
    )

    // Use overridden baseUrl if provided, otherwise use config baseUrl
    private val effectiveBaseUrl: String
        get() = baseUrlOverride ?: config.baseUrl

    /**
     * Gets template content, falling back to built-in template if file doesn't exist
     */
    private fun getTemplateContent(templateName: String, fallbackTemplate: String): String {
        val templateFile = templatesPath.resolve("$templateName.${templateEngine.extension}")
        return if (Files.exists(templateFile)) {
            templateFile.readText()
        } else {
            println("Template $templateName.${templateEngine.extension} not found, using built-in fallback")
            fallbackTemplate
        }
    }

    fun generate() {
        val posts = loadBlogPosts().sortedByDescending { it.date }
        val pages = loadPages().sortedWith(compareBy<SitePage> { it.navOrder ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() })

        val datasourceBundle = datasourceGenerator.buildBundle(posts, pages)
        val datasourceContext = datasourceBundle.toTemplateContext()

        generateHomePage(posts, pages, datasourceContext)
        generateBlogPosts(posts, pages, datasourceContext)
        generatePages(pages, datasourceContext)
        rssGenerator.generate(posts)
        copyStaticAssets()
        datasourceGenerator.writeBundle(datasourceBundle)
    }

    private fun loadBlogPosts(): List<BlogPost> {
        val postsDirectory = config.paths.posts
        return fileWalker.walkMarkdownFiles(postsDirectory, excludeIndex = true)
            .map { file ->
                val parsedPost = contentProcessor.process(file)
                val title = parsedPost.metadata["title"] ?: file.nameWithoutExtension
                val date = parsedPost.metadata["published"]?.let { LocalDateTime.parse(it) } ?: Files.getLastModifiedTime(file).let { LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) }
                val layout = parsedPost.metadata["layout"]

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
            }.toList()
    }

    private fun loadPages(): List<SitePage> {
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
            .toList()
    }

    private fun copyStaticAssets() {
        val assetsRoot = Paths.get(rootPath, config.theme.assets)
        fileWalker.walkStaticFiles(config.theme.assets)
            .forEach { source ->
                val relativePath = assetsRoot.relativize(source)
                val destination = Paths.get(rootPath, config.theme.output).resolve(relativePath)
                Files.createDirectories(destination.parent)
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
    }


    private fun generateBlogPosts(
        posts: List<BlogPost>,
        pages: List<SitePage>,
        datasourceContext: Map<String, Any?>
    ) {
        val templateContent = getTemplateContent("post", FallbackTemplates.POST_TEMPLATE)

        posts.forEach { post ->
            // Use the post template if not a template file, otherwise wrap in layout
            val html = if (post.isTemplate) {
                // For template files, use layout if specified in metadata or default
                val layout = post.metadata["layout"] ?: "default"
                val description: String = post.metadata["description"] ?: post.title
                templateEngine.renderWithLayout(
                    post.content,
                    mapOf(
                        "post" to post,
                        "baseUrl" to effectiveBaseUrl,
                        "siteName" to config.siteName,
                        "pages" to pages,
                        "title" to post.title,
                        "description" to description,
                        "layout" to layout
                    ).withDatasource(datasourceContext)
                )
            } else {
                val layout = post.metadata["layout"] ?: "default"
                templateEngine.renderWithLayout(
                    templateContent,
                    mapOf(
                        "post" to post,
                        "baseUrl" to effectiveBaseUrl,
                        "siteName" to config.siteName,
                        "pages" to pages,
                        "title" to post.title,
                        "description" to post.content.take(160),
                        "layout" to layout
                    ).withDatasource(datasourceContext)
                )
            }

            val outputPath = Paths.get(rootPath, config.theme.output, post.path, "index.html")
            outputPath.parent.createDirectories()
            Files.writeString(outputPath, html)
        }
    }

    private fun generateHomePage(
        posts: List<BlogPost>,
        pages: List<SitePage>,
        datasourceContext: Map<String, Any?>
    ) {
        val templateContent = getTemplateContent("home", FallbackTemplates.HOME_TEMPLATE)

        val html = templateEngine.renderWithLayout(
            templateContent,
            mapOf(
                "posts" to posts,
                "siteName" to config.siteName,
                "description" to config.description,
                "baseUrl" to effectiveBaseUrl,
                "pages" to pages,
                "featuredPage" to pages.firstOrNull { it.path.isNotEmpty() },
                "layout" to "default"
            ).withDatasource(datasourceContext)
        )

        val outputPath = Paths.get(rootPath, config.theme.output, "index.html")
        outputPath.parent.createDirectories()
        Files.writeString(outputPath, html)
    }

    private fun generatePages(
        pages: List<SitePage>,
        datasourceContext: Map<String, Any?>
    ) {
        val templateContent = getTemplateContent("page", FallbackTemplates.PAGE_TEMPLATE)

        val outputRoot = Paths.get(rootPath, config.theme.output)

        pages.forEach { page ->
            val html = if (page.isTemplate) {
                // For template files, render directly with layout
                val layout = page.metadata["layout"] ?: "default"
                val description: String = page.metadata["description"] ?: config.description
                templateEngine.renderWithLayout(
                    page.content,
                    mapOf(
                        "page" to page,
                        "pages" to pages,
                        "baseUrl" to effectiveBaseUrl,
                        "siteName" to config.siteName,
                        "description" to description,
                        "title" to page.title,
                        "layout" to layout
                    ).withDatasource(datasourceContext)
                )
            } else {
                val layout = page.metadata["layout"] ?: "default"
                templateEngine.renderWithLayout(
                    templateContent,
                    mapOf(
                        "page" to page,
                        "pages" to pages,
                        "baseUrl" to effectiveBaseUrl,
                        "siteName" to config.siteName,
                        "description" to config.description,
                        "title" to page.title,
                        "layout" to layout
                    ).withDatasource(datasourceContext)
                )
            }

            val pageOutputDir = if (page.path.isNotEmpty()) {
                outputRoot.resolve(page.path)
            } else {
                outputRoot
            }

            val outputPath = pageOutputDir.resolve("index.html")

            outputPath.parent.createDirectories()
            Files.writeString(outputPath, html)
        }
    }

    private fun Map<String, Any?>.withDatasource(datasource: Map<String, Any?>): Map<String, Any?> =
        this + ("datasource" to datasource)

}
