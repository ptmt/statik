package com.potomushto.statik.processors

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/**
 * Processes content files of different types (.md, .html, .hbs)
 * Extracts frontmatter metadata and content
 */
class ContentProcessor(private val markdownProcessor: MarkdownProcessor) {

    /**
     * Process a content file based on its extension
     */
    fun process(file: Path): ParsedPost {
        return when (file.extension.lowercase()) {
            "md" -> processMarkdown(file)
            "html" -> processHtml(file)
            "hbs" -> processHandlebars(file)
            else -> throw IllegalArgumentException("Unsupported file type: ${file.extension}")
        }
    }

    private fun processMarkdown(file: Path): ParsedPost {
        return markdownProcessor.process(file.readText())
    }

    private fun processHtml(file: Path): ParsedPost {
        val content = file.readText()
        val parsed = extractFrontmatter(content)
        // Post-process HTML to wrap blockquotes with data-author in figure/figcaption
        val processedContent = markdownProcessor.processHtmlBlockquotes(parsed.content)
        return ParsedPost(processedContent, parsed.metadata)
    }

    private fun processHandlebars(file: Path): ParsedPost {
        val content = file.readText()
        // For .hbs files, we return the raw content with metadata
        // The template will be rendered later with the template engine
        return extractFrontmatter(content)
    }

    /**
     * Extract YAML frontmatter from HTML or HBS files
     * Frontmatter is delimited by --- at the start of the file
     */
    private fun extractFrontmatter(content: String): ParsedPost {
        val frontmatterRegex = Regex("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", RegexOption.DOT_MATCHES_ALL)
        val match = frontmatterRegex.find(content)

        if (match != null) {
            val yamlContent = match.groupValues[1]
            val bodyContent = match.groupValues[2]
            val metadata = parseYaml(yamlContent)
            return ParsedPost(bodyContent, metadata)
        }

        // No frontmatter, return content as-is with empty metadata
        return ParsedPost(content, emptyMap())
    }

    /**
     * Simple YAML parser for frontmatter
     * Supports key: value pairs
     */
    private fun parseYaml(yaml: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        yaml.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && trimmed.contains(':')) {
                val parts = trimmed.split(':', limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                    metadata[key] = value
                }
            }
        }
        return metadata
    }
}
