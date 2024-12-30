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
    private val markdownProcessor = MarkdownProcessor()
    private val templateEngine = HandlebarsTemplateEngine(rootPath)
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
                println(">> parse $file ${parsedPost.content} ${parsedPost.metadata} $title $date")
                BlogPost(
                    id = file.nameWithoutExtension,
                    title = title,
                    date = date,
                    content = parsedPost.content,
                    rawHtml = null,
                    metadata = parsedPost.metadata,
                    path = fileWalker.generatePath(file)
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
        val template = templateEngine.compile("post")

        posts.forEach { post ->
            val html = template.apply(mapOf(
                "post" to post,
                "baseUrl" to config.baseUrl
            ))

            val outputPath = Paths.get(rootPath, config.theme.output, post.path, "index.html")
            outputPath.parent.createDirectories()
            Files.writeString(outputPath, html)
        }
    }

    private fun generateHomePage(posts: List<BlogPost>) {
        val template = templateEngine.compile("home")

        val html = template.apply(mapOf(
            "posts" to posts,
            "siteName" to config.siteName,
            "baseUrl" to config.baseUrl
        ))

        val outputPath = Paths.get(rootPath, config.theme.output, "index.html")
        outputPath.parent.createDirectories()
        Files.writeString(outputPath, html)
    }
}
