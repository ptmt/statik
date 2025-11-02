package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class ExcerptHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("excerpt", Helper<String> { value, options ->
            if (value == null) {
                ""
            } else {
                val plainText = value.replace(Regex("<h[1-6].*?>(.*?)</h[1-6]>"), "$1 ")
                    .replace(Regex("<[^>]*>"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val words = options?.hash?.get("words") as? Int ?: 30
                val tokens = plainText.split(" ")
                val truncated = tokens.take(words).joinToString(" ")
                val ellipsis = if (tokens.size > words) "..." else ""
                truncated + ellipsis
            }
        })
    }
}
