package com.potomushto.statik.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.readText

class HandlebarsTemplateEngine(val templatesPath: Path) : TemplateEngine {
    override val extension = "hbs"

    private val handlebars: Handlebars = Handlebars().prettyPrint(true)
    private val layoutCache = mutableMapOf<String, String>()

    fun registerHelper(name: String, helper: Helper<*>) {
        handlebars.registerHelper(name, helper)
    }

    override fun registerPartial(name: String, partial: String) {
        // Partials are loaded via the include helper dynamically
        // No need to pre-register them
    }

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
                if (context == null) return ""
                
                // Strip HTML tags but preserve content
                val plainText = context.replace(Regex("<h[1-6].*?>(.*?)</h[1-6]>"), "$1 ")
                    .replace(Regex("<[^>]*>"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                
                val words = options?.hash?.get("words") as? Int ?: 30
                return plainText.split(" ")
                    .take(words)
                    .joinToString(" ")
                    .plus(if (plainText.split(" ").size > words) "..." else "")
            }
        })
        
        // Add a helper to render HTML content safely
        registerHelper("safe", object: Helper<String> {
            override fun apply(context: String?, options: com.github.jknack.handlebars.Options?): Any? {
                return context?.let {
                    com.github.jknack.handlebars.Handlebars.SafeString(it)
                } ?: ""
            }
        })

        registerHelper("include", object: Helper<String> {
            override fun apply(context: String?, options: com.github.jknack.handlebars.Options?): CharSequence {
                try {
                    val fileName = context ?: error("missing template name")
                    val file = templatesPath.resolve(fileName).toFile()
                    return if (file.exists()) {
                        val template = handlebars.compileInline(file.readText())
                        Handlebars.SafeString(template.apply(options!!.context))
                    } else {
                        Handlebars.SafeString("<!-- File not found: $fileName -->")
                    }
                } catch (e: Exception) {
                    return Handlebars.SafeString("<!-- Error including file: ${e.message} -->")
                }
            }
        })

        registerHelper("eq", object: Helper<Any> {
            override fun apply(context: Any?, options: com.github.jknack.handlebars.Options?): CharSequence {
                val param = options?.param<Any>(0)
                return if (context == param) {
                    options?.fn() ?: ""
                } else {
                    options?.inverse() ?: ""
                }
            }
        })

        registerHelper("substring", object: Helper<String> {
            override fun apply(context: String?, options: com.github.jknack.handlebars.Options?): Any? {
                if (context == null) return ""

                return try {
                    val start = options?.param<Int>(0) ?: 0
                    val end = if (options != null && options.params.size > 1) {
                        options.param<Int>(1)
                    } else {
                        context.length
                    }

                    val safeStart = start.coerceIn(0, context.length)
                    val safeEnd = end.coerceIn(0, context.length)

                    if (safeStart >= safeEnd) {
                        ""
                    } else {
                        context.substring(safeStart, safeEnd)
                    }
                } catch (e: Exception) {
                    ""
                }
            }
        })

        registerHelper("groupBy", object: Helper<List<*>> {
            override fun apply(context: List<*>?, options: com.github.jknack.handlebars.Options?): Any? {
                if (context == null) return emptyList<Map<String, Any>>()

                val key = options?.param<String>(0) ?: return context

                // Group items by the specified metadata key
                val grouped = context.groupBy { item ->
                    when (item) {
                        is Map<*, *> -> {
                            val metadata = item["metadata"] as? Map<*, *>
                            metadata?.get(key) as? String ?: ""
                        }
                        else -> ""
                    }
                }

                // Return list of groups with name and items
                return grouped.map { (groupName, items) ->
                    mapOf(
                        "name" to groupName,
                        "items" to items
                    )
                }
            }
        })

        registerHelper("sortBy", object: Helper<List<*>> {
            override fun apply(context: List<*>?, options: com.github.jknack.handlebars.Options?): Any? {
                if (context == null) return emptyList<Any>()

                val key = options?.param<String>(0) ?: return context

                // Sort items by the specified metadata key
                return context.sortedBy { item ->
                    when (item) {
                        is Map<*, *> -> {
                            val metadata = item["metadata"] as? Map<*, *>
                            val value = metadata?.get(key)
                            when (value) {
                                is Number -> value.toInt()
                                is String -> value.toIntOrNull() ?: 0
                                else -> 0
                            }
                        }
                        else -> 0
                    }
                }
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

    /**
     * Load a layout template from the layouts directory
     */
    private fun loadLayout(layoutName: String): String? {
        if (layoutCache.containsKey(layoutName)) {
            return layoutCache[layoutName]
        }

        val layoutPath = templatesPath.resolve("layouts").resolve("$layoutName.$extension")
        return if (Files.exists(layoutPath)) {
            val content = layoutPath.readText()
            layoutCache[layoutName] = content
            content
        } else {
            null
        }
    }

    /**
     * Render a template with an optional layout wrapper
     * If layout is specified in data, wraps the template content in the layout
     * Falls back to "default" layout if the specified layout is not found
     */
    override fun renderWithLayout(template: String, data: Map<String, Any?>): String {
        val layoutName = data["layout"] as? String

        // First, render the content template
        val contentHtml = render(template, data)

        // If no layout specified, return content as-is
        if (layoutName == null) {
            return contentHtml
        }

        // Load and apply layout
        var layoutTemplate = loadLayout(layoutName)

        // If layout not found and it's not already "default", try falling back to default
        if (layoutTemplate == null && layoutName != "default") {
            println("Layout '$layoutName' not found, falling back to 'default' layout")
            layoutTemplate = loadLayout("default")
        }

        if (layoutTemplate != null) {
            // Create new data map with content injected
            val layoutData = data.toMutableMap()
            layoutData["content"] = contentHtml
            return render(layoutTemplate, layoutData)
        }

        // If no layout found at all, return content without layout
        return contentHtml
    }
} 