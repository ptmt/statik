package com.potomushto.statik.processors

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
            // CustomHtmlExtension.create()
        ))
        .build()

    private val renderer = HtmlRenderer.builder()
        .extensions(listOf(
            TablesExtension.create(),
            // CustomHtmlExtension.create()
        ))
        .build()

    val yamlVisitor = AbstractYamlFrontMatterVisitor()

    fun process(content: String): ParsedPost {
        val document = parser.parse(content)
        yamlVisitor.visit(document)
        yamlVisitor.data.forEach { (key, value) ->
            println("$key: $value")
        }
        return ParsedPost(
            renderer.render(document),
            emptyMap()
        )
    }
}

class ParsedPost(
    val content: String,
    val metadata: Map<String, String>
)