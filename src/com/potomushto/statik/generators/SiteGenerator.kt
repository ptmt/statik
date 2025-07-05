package com.potomushto.statik.generators

import com.potomushto.statik.models.BlogPost
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
        val posts = loadBlogPosts()
        generateHomePage(posts)
//        generateArchivePages(posts)
        generateBlogPosts(posts)
//        generateRssFeed(posts)
//        copyStaticAssets()
    }

    private fun loadBlogPosts(): List<BlogPost> {
        return fileWalker.walkBlogFiles()
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
                    outputPath = fileWalker.generatePath(file)
                )
            }.toList()
    }

    private fun copyStaticAssets() {
        fileWalker.walkStaticFiles(config.theme.assets)
            .forEach { source ->
                val relativePath = Paths.get(config.theme.assets).relativize(source)
                val destination = Paths.get(rootPath, config.theme.output).resolve(relativePath)
                Files.createDirectories(destination.parent)
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    private fun generateRssFeed(posts: List<BlogPost>) {
        //RssGenerator(config).generate(posts)
    }

    private fun generateBlogPosts(posts: List<BlogPost>) {
        val templateFile = templatesPath.resolve( "post.${templateEngine.extension}")
        val template = templateEngine.compile(templateFile.readText())

        posts.forEach { post ->
            val html = template(mapOf(
                "post" to post,
                "baseUrl" to config.baseUrl,
                "siteName" to config.siteName
            ))

            val outputPath = Paths.get(rootPath, config.theme.output, post.path, "index.html")
            outputPath.parent.createDirectories()
            Files.writeString(outputPath, html)
        }
    }

    private fun generateHomePage(posts: List<BlogPost>) {
        val templateFile = templatesPath.resolve("home.${templateEngine.extension}")
        val template = templateEngine.compile(templateFile.readText())

        val html = template(mapOf(
            "posts" to posts,
            "siteName" to config.siteName,
            "description" to config.description,
            "baseUrl" to config.baseUrl
        ))

        val outputPath = Paths.get(rootPath, config.theme.output, "index.html")
        outputPath.parent.createDirectories()
        Files.writeString(outputPath, html)
    }
}
