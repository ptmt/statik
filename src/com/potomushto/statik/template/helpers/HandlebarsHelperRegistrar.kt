package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import java.nio.file.Path

interface HandlebarsHelperRegistrar {
    fun register(handlebars: Handlebars, context: HelperRegistrationContext)
}

data class HelperRegistrationContext(
    val templatesPath: Path,
    val blockRegistry: ThreadLocal<MutableMap<String, MutableList<CharSequence>>?>
)
