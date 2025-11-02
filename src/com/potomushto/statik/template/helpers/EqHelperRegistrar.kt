package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class EqHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("eq", Helper<Any> { value, options ->
            val param = options?.param<Any>(0)
            value == param
        })
    }
}
