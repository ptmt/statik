package com.potomushto.statik.processors

import com.vladsch.flexmark.ast.AutoLink
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser

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
        .build()


    val yamlVisitor = AbstractYamlFrontMatterVisitor()

    fun process(content: String): ParsedPost {
        val document = parser.parse(content)
        yamlVisitor.visit(document)
        
        // Debug output
        yamlVisitor.data.forEach { (key, value) ->
            println("YAML: $key: $value")
        }
        
        // Convert the YAML visitor data to a Map<String, String>
        // The visitor returns List<String> for each key, we take the first value
        val metadata = yamlVisitor.data.mapValues { (_, values) -> 
            values.firstOrNull() ?: ""
        }
        
        return ParsedPost(
            renderer.render(document),
            metadata
        )
    }
}

class ParsedPost(
    val content: String,
    val metadata: Map<String, String>
)