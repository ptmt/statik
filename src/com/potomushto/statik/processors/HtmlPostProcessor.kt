package com.potomushto.statik.processors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Post-processes HTML content to apply semantic transformations.
 * This phase runs after Markdown rendering or HTML parsing.
 *
 * Transformations include:
 * - Wrapping blockquotes with data-author in figure/figcaption
 * - Future transformations can be added here
 */
class HtmlPostProcessor {

    /**
     * Apply all post-processing transformations to HTML content
     */
    fun process(html: String): String {
        val doc = Jsoup.parse(html)

        wrapBlockquotesWithAttribution(doc)
        // Future transformations can be added here

        return doc.body().html()
    }

    /**
     * Wrap blockquotes with data-author attribute in figure/figcaption elements
     * for semantic HTML and proper attribution display.
     */
    private fun wrapBlockquotesWithAttribution(doc: Document) {
        doc.select("blockquote[data-author]").forEach { blockquote ->
            val author = blockquote.attr("data-author")
            if (author.isNotBlank()) {
                // Create figure and figcaption
                val figure = doc.createElement("figure")
                val figcaption = doc.createElement("figcaption")
                figcaption.text("— $author")

                // Move blockquote into figure
                blockquote.before(figure)
                figure.appendChild(blockquote.clone())
                figure.appendChild(figcaption)
                blockquote.remove()
            }
        }
    }
}
