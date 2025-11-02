package com.potomushto.statik.template

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * HTML beautifier that formats HTML with proper indentation.
 * Uses jsoup (already a project dependency) for parsing and formatting.
 */
class HtmlBeautifier(private val indentSize: Int = 2) : HtmlProcessor {

    override fun process(html: String): String {
        // Parse the HTML with jsoup
        val document = Jsoup.parse(html)

        // Configure output settings for pretty printing
        document.outputSettings()
            .prettyPrint(true)
            .indentAmount(indentSize)
            .outline(false) // Don't add extra newlines

        // Return the formatted HTML
        return document.html()
    }
}