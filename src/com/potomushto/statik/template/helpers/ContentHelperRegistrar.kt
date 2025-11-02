package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class ContentHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("content", Helper<String> { name, options ->
            val blocks = context.blockRegistry.get() ?: return@Helper Handlebars.SafeString("")
            val blockName = name ?: return@Helper Handlebars.SafeString("")
            val slot = blocks.getOrPut(blockName) { mutableListOf() }
            options.fn()?.let { slot.add(it) }
            Handlebars.SafeString("")
        })
    }
}
