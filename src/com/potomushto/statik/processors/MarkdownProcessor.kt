package com.potomushto.statik.processors

import com.potomushto.statik.logging.LoggerFactory
import com.vladsch.flexmark.ast.AutoLink
import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.Link
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
