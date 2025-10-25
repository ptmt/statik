package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.models.BlogPost
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories

class RssGenerator(
    private val config: BlogConfig,
    private val outputPath: Path,
    private val baseUrlOverride: String? = null
) {
    private val effectiveBaseUrl: String
        get() = baseUrlOverride ?: config.baseUrl

    private val rfc822Formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")
        .withZone(ZoneId.systemDefault())

    fun generate(posts: List<BlogPost>) {
        if (!config.rss.enabled) {
            return
        }

        val sortedPosts = posts
            .sortedByDescending { it.date }
            .take(config.rss.maxItems)

        val rssContent = buildRssXml(sortedPosts)
        val rssFile = outputPath.resolve(config.rss.fileName)

        rssFile.parent?.createDirectories()
        Files.writeString(rssFile, rssContent)
    }

    private fun buildRssXml(posts: List<BlogPost>): String {
        val feedTitle = config.rss.title ?: config.siteName
        val feedDescription = config.rss.description ?: config.description
        val buildDate = posts.firstOrNull()?.date?.atZone(ZoneId.systemDefault())
            ?.format(rfc822Formatter) ?: ""

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:content="http://purl.org/rss/1.0/modules/content/">""")
            appendLine("  <channel>")
            appendLine("    <title>${escapeXml(feedTitle)}</title>")
            appendLine("    <link>$effectiveBaseUrl</link>")
            appendLine("    <description>${escapeXml(feedDescription)}</description>")
            appendLine("    <language>${config.rss.language}</language>")
            if (buildDate.isNotEmpty()) {
                appendLine("    <lastBuildDate>$buildDate</lastBuildDate>")
            }
            appendLine("""    <atom:link href="$effectiveBaseUrl/${config.rss.fileName}" rel="self" type="application/rss+xml"/>""")
            appendLine()

            posts.forEach { post ->
                appendLine("    <item>")
                appendLine("      <title>${escapeXml(post.title)}</title>")
                appendLine("      <link>$effectiveBaseUrl/${post.path}</link>")
                appendLine("      <guid isPermaLink=\"true\">$effectiveBaseUrl/${post.path}</guid>")

                val pubDate = post.date.atZone(ZoneId.systemDefault()).format(rfc822Formatter)
                appendLine("      <pubDate>$pubDate</pubDate>")

                if (config.author.isNotEmpty()) {
                    appendLine("      <author>${escapeXml(config.author)}</author>")
                }

                // Add description
                val description = post.metadata["description"]
                    ?: post.content.take(300).replace(Regex("<[^>]*>"), "").trim()
                appendLine("      <description>${escapeXml(description)}</description>")

                // Optionally include full content
                if (config.rss.includeFullContent) {
                    appendLine("      <content:encoded><![CDATA[${post.content}]]></content:encoded>")
                }

                appendLine("    </item>")
                appendLine()
            }

            appendLine("  </channel>")
            appendLine("</rss>")
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
