package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import java.time.LocalDateTime

class FormatDateHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("formatDate", Helper<LocalDateTime> { value, options ->
            value?.let {
                val formatter = options?.hash?.get("format") as? String ?: "MMMM dd, yyyy"
                val dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern(formatter)
                it.format(dateTimeFormatter)
            }
        })
    }
}
