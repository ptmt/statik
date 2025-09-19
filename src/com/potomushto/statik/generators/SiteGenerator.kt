package com.potomushto.statik.generators

import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import kotlin.io.path.readText
import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.processors.MarkdownProcessor
import com.potomushto.statik.template.HandlebarsTemplateEngine
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension

class SiteGenerator(private val rootPath: String,
                    private val config: BlogConfig) {
    private val templatesPath = Paths.get(rootPath, config.theme.templates)
    private val markdownProcessor = MarkdownProcessor()
    private val templateEngine = HandlebarsTemplateEngine(templatesPath)
  //  private val rssGenerator = RssGenerator()
    private val fileWalker = FileWalker(rootPath)

    fun generate() {
        val posts = loadBlogPosts().sortedByDescending { it.date }
        val pages = loadPages().sortedWith(compareBy<SitePage> { it.navOrder ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() })

        generateHomePage(posts, pages)
//        generateArchivePages(posts)
        generateBlogPosts(posts, pages)
        generatePages(pages)
//        generateRssFeed(posts)
        copyStaticAssets()
    }

    private fun loadBlogPosts(): List<BlogPost> {
        val postsDirectory = config.paths.posts
        return fileWalker.walkMarkdownFiles(postsDirectory)
            .map { file ->
                val postContent = file.readText()
                val parsedPost = markdownProcessor.process(postContent)
                val title = parsedPost.metadata["title"] ?: file.nameWithoutExtension
                val date = parsedPost.metadata["published"]?.let { LocalDateTime.parse(it) } ?: Files.getLastModifiedTime(file).let { LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) }
                BlogPost(
                    id = file.nameWithoutExtension,
                    title = title,
                    date = date,
                    content = parsedPost.content,
                    rawHtml = null,
                    metadata = parsedPost.metadata,
                    outputPath = fileWalker.generatePath(file, postsDirectory)
                )
            }.toList()
    }

    private fun loadPages(): List<SitePage> {
        val pagesDirectory = config.paths.pages
        return fileWalker.walkMarkdownFiles(pagesDirectory)
            .map { file ->
                val pageContent = file.readText()
                val parsedPage = markdownProcessor.process(pageContent)
                val title = parsedPage.metadata["title"] ?: file.nameWithoutExtension
                val navOrder = parsedPage.metadata["nav_order"]?.toIntOrNull()
                    ?: parsedPage.metadata["navOrder"]?.toIntOrNull()

                SitePage(
                    id = file.nameWithoutExtension,
                    title = title,
                    content = parsedPage.content,
                    metadata = parsedPage.metadata,
                    outputPath = fileWalker.generatePath(file, pagesDirectory, stripIndex = true),
                    navOrder = navOrder
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

    private fun generateRssFeed(posts: List<BlogPost>) {
        //RssGenerator(config).generate(posts)
    }

    private fun generateBlogPosts(posts: List<BlogPost>, pages: List<SitePage>) {
        val templateFile = templatesPath.resolve( "post.${templateEngine.extension}")
        val template = templateEngine.compile(templateFile.readText())

        posts.forEach { post ->
            val html = template(mapOf(
                "post" to post,
                "baseUrl" to config.baseUrl,
                "siteName" to config.siteName,
                "pages" to pages
            ))

            val outputPath = Paths.get(rootPath, config.theme.output, post.path, "index.html")
            outputPath.parent.createDirectories()
            Files.writeString(outputPath, html)
        }
    }

    private fun generateHomePage(posts: List<BlogPost>, pages: List<SitePage>) {
        val templateFile = templatesPath.resolve("home.${templateEngine.extension}")
        val template = templateEngine.compile(templateFile.readText())

        val html = template(mapOf(
            "posts" to posts,
            "siteName" to config.siteName,
            "description" to config.description,
            "baseUrl" to config.baseUrl,
            "pages" to pages,
            "featuredPage" to pages.firstOrNull { it.path.isNotEmpty() }
        ))

        val outputPath = Paths.get(rootPath, config.theme.output, "index.html")
        outputPath.parent.createDirectories()
        Files.writeString(outputPath, html)
    }

    private fun generatePages(pages: List<SitePage>) {
        val templateFile = templatesPath.resolve("page.${templateEngine.extension}")
        if (!Files.exists(templateFile)) {
            return
        }

        val template = templateEngine.compile(templateFile.readText())

        val outputRoot = Paths.get(rootPath, config.theme.output)

        pages.forEach { page ->
            val html = template(
                mapOf(
                    "page" to page,
                    "pages" to pages,
                    "baseUrl" to config.baseUrl,
                    "siteName" to config.siteName,
                    "description" to config.description
                )
            )

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
}
