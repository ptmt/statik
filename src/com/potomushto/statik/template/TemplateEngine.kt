package com.potomushto.statik.template
import com.github.jknack.handlebars.Helper

interface TemplateEngine {
    val extension: String

    fun compile(template: String): (Map<String, Any?>) -> String
    fun render(template: String, data: Map<String, Any?>): String
    fun registerPartial(name: String, partial: String)

    /**
     * Render a template with optional layout support
     * If data contains "layout" key, wraps the template in the specified layout
     */
    fun renderWithLayout(template: String, data: Map<String, Any?>): String {
        return render(template, data)
    }
} 