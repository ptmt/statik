package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FormatDateHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("formatDate", Helper<LocalDateTime> { value, options ->
            value?.let {
                val formatter = when (val positional = options?.params?.getOrNull(0)) {
                    is String -> positional
                    is CharSequence -> positional.toString()
                    else -> options?.hash?.get("format") as? String
                } ?: "MMMM dd, yyyy"
                val dateTimeFormatter = DateTimeFormatter.ofPattern(formatter)
                it.format(dateTimeFormatter)
            }
        })
    }
}
