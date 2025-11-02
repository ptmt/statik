package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class SafeHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("safe", Helper<String> { value, _ ->
            value?.let { Handlebars.SafeString(it) } ?: ""
        })
    }
}
