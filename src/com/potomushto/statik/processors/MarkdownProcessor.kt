package com.potomushto.statik.processors

import com.potomushto.statik.logging.LoggerFactory
import com.vladsch.flexmark.ast.AutoLink
import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.AttributeProvider
import com.vladsch.flexmark.html.AttributeProviderFactory
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory
import com.vladsch.flexmark.html.renderer.AttributablePart
import com.vladsch.flexmark.html.renderer.LinkResolverContext
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.html.MutableAttributes

private val logger = LoggerFactory.getLogger(MarkdownProcessor::class.java)

class MarkdownProcessor {
    private val parser = Parser.builder()
        .extensions(listOf(
            TablesExtension.create(),
            YamlFrontMatterExtension.create(),
            FootnoteExtension.create(),
            AnchorLinkExtension.create(),
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
         //   CustomHtmlExtension.create()
        ))
        .build()

    private val renderer = HtmlRenderer.builder()
        .extensions(listOf(
            TablesExtension.create(),
            FootnoteExtension.create(),
            AnchorLinkExtension.create(),
            StrikethroughExtension.create(),
            // CustomHtmlExtension.create()
        ))
        .nodeRendererFactory(ImageCaptionNodeRenderer.Factory())
        .nodeRendererFactory(BlockQuoteAttributionNodeRenderer.Factory())
        .build()

    fun process(content: String): ParsedPost {
        val document = parser.parse(content)
        val yamlVisitor = AbstractYamlFrontMatterVisitor()
        yamlVisitor.visit(document)

        // Convert the YAML visitor data to a Map<String, String>
        // The visitor returns List<String> for each key, we take the first value
        val metadata = yamlVisitor.data.mapValues { (_, values) ->
            sanitizeMetadataValue(values.firstOrNull())
        }

        return ParsedPost(
            renderer.render(document),
            metadata
        )
    }

    /**
     * Post-process HTML content to wrap blockquotes with data-author attribute
     * in figure/figcaption elements for semantic HTML.
     */
    fun processHtmlBlockquotes(html: String): String {
        val doc = org.jsoup.Jsoup.parse(html)
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
        return doc.body().html()
    }

    private fun sanitizeMetadataValue(rawValue: String?): String {
        val value = rawValue?.trim() ?: return ""

        if (value.length >= 2) {
            val firstChar = value.first()
            val lastChar = value.last()
            if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
                return value.substring(1, value.length - 1).trim()
            }
        }

        return value
    }
}

class ParsedPost(
    val content: String,
    val metadata: Map<String, String>
)

/**
 * Custom NodeRenderer that handles images with captions.
 * If an image has a title attribute, it wraps it in <figure> with <figcaption>.
 */
class ImageCaptionNodeRenderer(val options: com.vladsch.flexmark.util.data.DataHolder) : NodeRenderer {
    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(Image::class.java) { node, context, html -> render(node, context, html) }
        )
    }

    private fun render(node: Image, context: NodeRendererContext, html: HtmlWriter) {
        val title = node.title.toString()
        val hasCaption = title.isNotEmpty()

        if (hasCaption) {
            html.tag("figure")
        }

        html.srcPos(node.chars).withAttr()
            .attr("src", context.encodeUrl(node.url.toString()))
            .attr("alt", node.text.toString())
            .tagVoidLine("img")

        if (hasCaption) {
            html.tag("figcaption")
            html.text(title)
            html.tag("/figcaption")
            html.tag("/figure")
        }
    }

    class Factory : NodeRendererFactory {
        override fun apply(options: com.vladsch.flexmark.util.data.DataHolder): NodeRenderer {
            return ImageCaptionNodeRenderer(options)
        }
    }
}

/**
 * Custom NodeRenderer that handles blockquotes with attribution.
 * Detects attribution patterns like "— Author" or "-- Author" in the last paragraph
 * and wraps the blockquote in <figure> with <figcaption>.
 */
class BlockQuoteAttributionNodeRenderer(val options: com.vladsch.flexmark.util.data.DataHolder) : NodeRenderer {
    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(BlockQuote::class.java) { node, context, html -> render(node, context, html) }
        )
    }

    private fun render(node: BlockQuote, context: NodeRendererContext, html: HtmlWriter) {
        // Check if the blockquote contains attribution
        val attribution = extractAttribution(node)
        val hasAttribution = attribution != null

        if (hasAttribution) {
            html.tag("figure")
        }

        // Render the blockquote
        html.line()

        if (hasAttribution) {
            // Add data-author attribute for datasource collection
            html.withAttr().attr("data-author", attribution!!).tag("blockquote")
        } else {
            html.tag("blockquote")
        }

        html.indent()

        // Render children
        var child = node.firstChild
        while (child != null) {
            if (child is Paragraph && hasAttribution && child.next == null) {
                // This is the last paragraph and has attribution - render without attribution line
                renderParagraphWithoutAttribution(child, context, html)
            } else {
                context.render(child)
            }
            child = child.next
        }

        html.unIndent().line().tag("/blockquote")

        if (hasAttribution) {
            html.tag("figcaption")
            html.text("— $attribution")
            html.tag("/figcaption")
            html.tag("/figure")
        }

        html.line()
    }

    private fun renderParagraphWithoutAttribution(paragraph: Paragraph, context: NodeRendererContext, html: HtmlWriter) {
        val text = paragraph.contentChars.toString()
        val lines = text.lines()

        // Remove the last line if it's an attribution line
        val contentLines = if (lines.isNotEmpty() && isAttributionLine(lines.last())) {
            lines.dropLast(1)
        } else {
            lines
        }

        val cleanText = contentLines.joinToString("\n").trim()

        if (cleanText.isNotEmpty()) {
            html.line().tag("p")
            html.text(cleanText)
            html.tag("/p").line()
        }
    }

    private fun extractAttribution(blockQuote: BlockQuote): String? {
        // Get all paragraphs in the blockquote
        val children = mutableListOf<Node>()
        var child = blockQuote.firstChild
        while (child != null) {
            if (child is Paragraph) {
                children.add(child)
            }
            child = child.next
        }

        if (children.isEmpty()) {
            return null
        }

        // Check the last paragraph for attribution at the end
        val lastParagraph = children.last() as Paragraph
        // Get the content chars (without markdown markers)
        val text = lastParagraph.contentChars.toString()

        // Look for attribution at the end of the text (can be on last line)
        val lines = text.lines()
        val lastLine = lines.lastOrNull()?.trim()

        if (lastLine == null) return null

        // Match patterns like "— Author" or "-- Author" at the end
        val emdashPattern = Regex("^[—–]\\s*(.+)$")
        val doubleHyphenPattern = Regex("^--\\s*(.+)$")

        val emdashMatch = emdashPattern.find(lastLine)
        if (emdashMatch != null) {
            return emdashMatch.groupValues[1].trim()
        }

        val hyphenMatch = doubleHyphenPattern.find(lastLine)
        if (hyphenMatch != null) {
            return hyphenMatch.groupValues[1].trim()
        }

        return null
    }

    private fun isAttributionLine(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.matches(Regex("^[—–]\\s*.+$")) || trimmed.matches(Regex("^--\\s*.+$"))
    }

    class Factory : NodeRendererFactory {
        override fun apply(options: com.vladsch.flexmark.util.data.DataHolder): NodeRenderer {
            return BlockQuoteAttributionNodeRenderer(options)
        }
    }
}
