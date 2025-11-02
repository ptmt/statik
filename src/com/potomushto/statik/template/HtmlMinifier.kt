package com.potomushto.statik.template

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * HTML minifier that uses jsoup for proper HTML parsing and minification.
 * Much safer than regex-based approaches as it understands HTML structure.
 *
 * Features:
 * - Properly parses HTML using jsoup's DOM parser
 * - Removes unnecessary whitespace between elements
 * - Preserves whitespace in <pre>, <code>, <textarea>, <script>, and <style> tags
 * - Handles edge cases like inline event handlers and CDATA sections correctly
 */
class HtmlMinifier : HtmlProcessor {

    override fun process(html: String): String {
        // Parse the HTML with jsoup
        val document = Jsoup.parse(html)

        // Configure output settings for minification
        document.outputSettings()
            .prettyPrint(false)           // Disable pretty printing
            .indentAmount(0)              // No indentation
            .outline(false)               // Don't add extra newlines

        // Collapse whitespace in text nodes (except in pre/code/textarea/script/style)
        collapseWhitespace(document)

        return document.html()
    }

    /**
     * Collapse excessive whitespace in text nodes while preserving
     * whitespace in certain tags where it's semantically important
     */
    private fun collapseWhitespace(document: Document) {
        val whitespacePreservingTags = setOf("pre", "code", "textarea", "script", "style")

        val body = document.body()
        if (body != null) {
            val textNodes = mutableListOf<TextNode>()

            // Collect all text nodes first
            body.traverse { node, _ ->
                if (node is TextNode) {
                    textNodes.add(node)
                }
            }

            // Process each text node
            for (textNode in textNodes) {
                // Check if this text node is inside a whitespace-preserving tag
                var parent = textNode.parent()
                var preserveWhitespace = false

                while (parent != null) {
                    if (parent is Element && whitespacePreservingTags.contains(parent.tagName().lowercase())) {
                        preserveWhitespace = true
                        break
                    }
                    parent = parent.parent()
                }

                if (preserveWhitespace.not()) {
                    // Collapse multiple whitespace characters to single space
                    val text = textNode.wholeText
                    val collapsed = text.replace(Regex("\\s+"), " ")

                    // If the text is only whitespace between tags, remove it entirely
                    if (collapsed.trim().isEmpty()) {
                        textNode.text("")
                    } else {
                        textNode.text(collapsed)
                    }
                }
            }
        }
    }
}