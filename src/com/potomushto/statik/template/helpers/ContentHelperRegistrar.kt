package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class ContentHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("content", Helper<Any?> { contextValue, options ->
            if (options.fn != null && (contextValue is String || contextValue is CharSequence)) {
                val blocks = context.blockRegistry.get() ?: return@Helper Handlebars.SafeString("")
                val blockName = contextValue.toString()
                val slot = blocks.getOrPut(blockName) { mutableListOf() }
                options.fn()?.let { slot.add(it) }
                return@Helper Handlebars.SafeString("")
            }

            val value = when (contextValue) {
                is Map<*, *> -> contextValue["content"] ?: contextValue
                else -> contextValue
            }

            when (value) {
                null -> Handlebars.SafeString("")
                is Handlebars.SafeString -> value
                else -> Handlebars.SafeString(value.toString())
            }
        })
    }
}
