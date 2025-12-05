package com.potomushto.statik.processors

import com.potomushto.statik.config.FootnoteDisplay
import com.potomushto.statik.config.FootnotesConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Post-processes HTML content to apply semantic transformations.
 * This phase runs after Markdown rendering or HTML parsing.
 *
 * Transformations include:
 * - Wrapping blockquotes with data-author in figure/figcaption
 * - Future transformations can be added here
 */
class HtmlPostProcessor(
    private val footnotesConfig: FootnotesConfig = FootnotesConfig()
) {

    /**
     * Apply all post-processing transformations to HTML content
     */
    fun process(html: String): String {
        val doc = Jsoup.parse(html)

        wrapBlockquotesWithAttribution(doc)
        applyFootnoteDisplayPreference(doc)
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

    private fun applyFootnoteDisplayPreference(doc: Document) {
        if (footnotesConfig.display != FootnoteDisplay.HOVER) {
            return
        }

        convertFootnotesToHover(doc)
    }

    private fun convertFootnotesToHover(doc: Document) {
        val footnotesContainer = doc.selectFirst("div.footnotes") ?: return
        val definitions = collectFootnoteDefinitions(footnotesContainer)
        if (definitions.isEmpty()) {
            footnotesContainer.remove()
            return
        }

        doc.select("a.footnote-ref").forEach { reference ->
            val targetId = reference.attr("href").removePrefix("#")
            val definition = definitions[targetId] ?: return@forEach

            val hostElement = reference.parent()?.takeIf { it.tagName() == "sup" }
                ?: reference

            if (hostElement === reference) {
                reference.tagName("sup")
            }

            hostElement.empty()
            hostElement.addClass("footnote-inline")
            hostElement.addClass("footnote-inline-hover")
            hostElement.attr("data-footnote-id", targetId)

            if (definition.text.isNotBlank()) {
                hostElement.attr("data-footnote", definition.text)
                hostElement.attr("title", definition.text)
            } else {
                hostElement.removeAttr("data-footnote")
                hostElement.removeAttr("title")
            }

            if (definition.html.isNotBlank()) {
                hostElement.attr("data-footnote-html", definition.html)
            } else {
                hostElement.removeAttr("data-footnote-html")
            }

            hostElement.attr("role", "doc-noteref")
            hostElement.attr("tabindex", "0")

            val label = reference.text().ifBlank { targetId }
            hostElement.text("[${label}]")
        }

        footnotesContainer.remove()
    }

    private fun collectFootnoteDefinitions(container: Element): Map<String, FootnoteDefinition> {
        return container.select("ol > li[id]").associate { definition ->
            val cleaned = definition.clone()
            cleaned.select("a.footnote-backref").remove()
            val html = cleaned.html().trim()
            val text = cleaned.text().replace("\n", " ").replace(Regex("\\s+"), " ").trim()

            definition.id() to FootnoteDefinition(html = html, text = text)
        }
    }

    private data class FootnoteDefinition(
        val html: String,
        val text: String
    )
}
