package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import java.nio.file.Files

class IncludeHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("include", Helper<String> { name, options ->
            try {
                val fileName = name ?: error("missing template name")
                val file = context.templatesPath.resolve(fileName)
                if (!Files.exists(file)) {
                    return@Helper Handlebars.SafeString("<!-- File not found: $fileName -->")
                }

                val template = handlebars.compileInline(Files.readString(file))
                Handlebars.SafeString(template.apply(options!!.context))
            } catch (e: Exception) {
                Handlebars.SafeString("<!-- Error including file: ${e.message} -->")
            }
        })
    }
}
