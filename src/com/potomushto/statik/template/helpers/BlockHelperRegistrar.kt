package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class BlockHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("block", Helper<String> { name, options ->
            val blocks = context.blockRegistry.get()
            val blockName = name ?: return@Helper options.fn() ?: ""
            val overrides = blocks?.get(blockName)?.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = "") { it.toString() }

            if (overrides != null) {
                return@Helper Handlebars.SafeString(overrides)
            }

            val fallback = options.fn()?.toString().orEmpty()
            Handlebars.SafeString(fallback)
        })
    }
}
