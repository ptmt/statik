package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class SubstringHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("substring", Helper<String> { value, options ->
            if (value == null) {
                ""
            } else {
                try {
                    val start = options?.param<Int>(0) ?: 0
                    val end = if (options != null && options.params.size > 1) {
                        options.param<Int>(1)
                    } else {
                        value.length
                    }

                    val safeStart = start.coerceIn(0, value.length)
                    val safeEnd = end.coerceIn(0, value.length)

                    if (safeStart >= safeEnd) "" else value.substring(safeStart, safeEnd)
                } catch (_: Exception) {
                    ""
                }
            }
        })
    }
}
