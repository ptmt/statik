package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser

class MarkdownHelperRegistrar : HandlebarsHelperRegistrar {
    private val parser = Parser.builder()
        .extensions(listOf(
            AutolinkExtension.create(),
            StrikethroughExtension.create()
        ))
        .build()

    private val renderer = HtmlRenderer.builder()
        .extensions(listOf(
            AutolinkExtension.create(),
            StrikethroughExtension.create()
        ))
        .build()

    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("md", Helper<Any?> { value, _ ->
            val text = value?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                return@Helper ""
            }

            val html = renderer.render(parser.parse(text))
            val inline = stripSingleParagraph(html)
            Handlebars.SafeString(inline)
        })
    }

    private fun stripSingleParagraph(html: String): String {
        val trimmed = html.trim()
        if (!trimmed.startsWith("<p>") || !trimmed.endsWith("</p>")) {
            return trimmed
        }

        val firstClose = trimmed.indexOf("</p>")
        if (firstClose != trimmed.length - 4) {
            return trimmed
        }

        val nextOpen = trimmed.indexOf("<p>", startIndex = 3)
        if (nextOpen != -1) {
            return trimmed
        }

        return trimmed.substring(3, trimmed.length - 4).trim()
    }
}
