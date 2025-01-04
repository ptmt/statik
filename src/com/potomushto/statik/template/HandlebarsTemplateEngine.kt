package com.potomushto.statik.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import java.time.LocalDateTime

class HandlebarsTemplateEngine : TemplateEngine {
    override val extension = "hbs"

    private val handlebars: Handlebars = Handlebars()

    init {
        registerHelper("formatDate", object: Helper<LocalDateTime> {
            override fun apply(context: LocalDateTime?, options: com.github.jknack.handlebars.Options?): Any? {
                return context?.let {
                    val formatter = options?.hash?.get("format") as? String ?: "MMMM dd, yyyy"
                    val dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern(formatter)
                    context.format(dateTimeFormatter)
                }
            }
        })
        registerHelper("excerpt", object: Helper<String> {
            override fun apply(context: String?, options: com.github.jknack.handlebars.Options?): Any? {
                val words = options?.hash?.get("words") as? Int ?: 30
                return context?.split(" ")?.take(words)?.joinToString(" ")?.plus("...")
            }
        })
    }

    override fun compile(template: String): (Map<String, Any?>) -> String {
        val compiledTemplate = handlebars.compileInline(template)
        return { data -> compiledTemplate.apply(data) }
    }

    override fun render(template: String, data: Map<String, Any?>): String {
        val compiledTemplate = compile(template)
        return compiledTemplate(data)
    }

    fun registerHelper(name: String, helper: Helper<*>) {
        handlebars.registerHelper(name, helper)
    }

    override fun registerPartial(name: String, partial: String) {
        TODO()
    }
} 