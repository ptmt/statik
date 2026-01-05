package com.potomushto.statik.processors

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/**
 * Processes content files of different types (.md, .html, .hbs)
 * Extracts frontmatter metadata and content
 */
class ContentProcessor(
    private val markdownProcessor: MarkdownProcessor,
    private val htmlPostProcessor: HtmlPostProcessor = HtmlPostProcessor()
) {

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
        val parsed = markdownProcessor.process(file.readText())
        // Post-process HTML for semantic transformations
        val processedContent = htmlPostProcessor.process(parsed.content)
        return ParsedPost(processedContent, parsed.metadata)
    }

    private fun processHtml(file: Path): ParsedPost {
        val content = file.readText()
        val parsed = extractFrontmatter(content)
        // Post-process HTML for semantic transformations
        val processedContent = htmlPostProcessor.process(parsed.content)
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
        return FrontmatterParser.extract(content)
    }
}
