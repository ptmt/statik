package com.potomushto.statik.template
import com.github.jknack.handlebars.Helper

interface TemplateEngine {
    val extension: String

    fun compile(template: String): (Map<String, Any?>) -> String
    fun render(template: String, data: Map<String, Any?>): String
    fun registerPartial(name: String, partial: String)
} 