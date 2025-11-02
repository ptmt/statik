package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class BlockHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("block", Helper<Any?> { name, options ->
            val blocks = context.blockRegistry.get()
            val blockName = when (name) {
                is String -> name
                is CharSequence -> name.toString()
                null -> null
                else -> name.toString()
            } ?: return@Helper options.fn() ?: ""
            val overrides = blocks?.get(blockName)?.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = "") { it.toString() }

            if (overrides != null) {
                return@Helper Handlebars.SafeString(overrides)
            }

            val renderedFallback = options.fn() ?: ""
            Handlebars.SafeString(renderedFallback)
        })
    }
}
