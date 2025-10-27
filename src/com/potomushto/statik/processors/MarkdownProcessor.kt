package com.potomushto.statik.processors

import com.potomushto.statik.logging.LoggerFactory
import com.vladsch.flexmark.ast.AutoLink
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
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory
import com.vladsch.flexmark.html.renderer.AttributablePart
import com.vladsch.flexmark.html.renderer.LinkResolverContext
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
        .attributeProviderFactory(LinkRewriterAttributeProvider.Factory())
        .build()


    val yamlVisitor = AbstractYamlFrontMatterVisitor()

    fun process(content: String): ParsedPost {
        val document = parser.parse(content)
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
 * AttributeProvider that rewrites internal links to remove /pages/ prefix
 * Converts /pages/foo to /foo for proper internal navigation
 */
class LinkRewriterAttributeProvider : AttributeProvider {
    override fun setAttributes(node: Node, part: AttributablePart, attributes: MutableAttributes) {
        if (node is Link && part == AttributablePart.LINK) {
            val href = attributes.getValue("href")
            if (href != null && href.startsWith("/pages/")) {
                // Rewrite /pages/foo to /foo
                val newHref = href.removePrefix("/pages")
                attributes.replaceValue("href", newHref)
            }
        }
    }

    class Factory : IndependentAttributeProviderFactory() {
        override fun apply(context: LinkResolverContext): AttributeProvider {
            return LinkRewriterAttributeProvider()
        }
    }
}
