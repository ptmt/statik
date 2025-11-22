package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

class DebugHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("debug", Helper<Any?> { _, options ->
            val sb = StringBuilder()
            sb.appendLine("<pre style=\"background: #f4f4f4; padding: 1em; border: 1px solid #ddd; overflow: auto; max-height: 600px;\">")
            sb.appendLine("<strong>Debug: Template Variables</strong>")
            sb.appendLine("═".repeat(60))

            // Get the current context - options.context gives us the data map
            val contextMap = options?.context?.model() as? Map<*, *>

            if (contextMap != null) {
                dumpVariables(contextMap, sb, "", 0, 3)
            } else {
                sb.appendLine("No context available")
            }

            sb.appendLine("═".repeat(60))
            sb.append("</pre>")

            // Return as SafeString to prevent HTML escaping
            Handlebars.SafeString(sb.toString())
        })
    }

    private fun dumpVariables(
        map: Map<*, *>,
        sb: StringBuilder,
        prefix: String,
        level: Int,
        maxDepth: Int
    ) {
        if (level > maxDepth) {
            sb.appendLine("$prefix... (max depth reached)")
            return
        }

        map.entries.sortedBy { it.key.toString() }.forEach { (key, value) ->
            val keyStr = key.toString()
            val indent = "  ".repeat(level)

            when (value) {
                null -> sb.appendLine("$indent$prefix$keyStr: null")
                is String -> {
                    val valueStr = if (value.length > 100) {
                        value.take(100) + "... (${value.length} chars)"
                    } else {
                        value
                    }
                    sb.appendLine("$indent$prefix$keyStr: \"$valueStr\"")
                }
                is Number -> sb.appendLine("$indent$prefix$keyStr: $value")
                is Boolean -> sb.appendLine("$indent$prefix$keyStr: $value")
                is List<*> -> {
                    sb.appendLine("$indent$prefix$keyStr: [${value.size} items]")
                    if (value.isNotEmpty() && level < maxDepth) {
                        value.take(3).forEachIndexed { index, item ->
                            when (item) {
                                is Map<*, *> -> {
                                    sb.appendLine("$indent  [$index]:")
                                    dumpVariables(item, sb, "", level + 2, maxDepth)
                                }
                                else -> sb.appendLine("$indent  [$index]: ${summarizeValue(item)}")
                            }
                        }
                        if (value.size > 3) {
                            sb.appendLine("$indent  ... (${value.size - 3} more items)")
                        }
                    }
                }
                is Map<*, *> -> {
                    sb.appendLine("$indent$prefix$keyStr: {${value.size} properties}")
                    if (level < maxDepth) {
                        dumpVariables(value, sb, "", level + 1, maxDepth)
                    }
                }
                else -> {
                    // Try to access properties of objects using reflection
                    val className = value::class.simpleName ?: "Unknown"
                    sb.appendLine("$indent$prefix$keyStr: [$className]")

                    if (level < maxDepth) {
                        try {
                            val properties = value::class.java.declaredFields
                                .filter { !it.isSynthetic }
                                .mapNotNull { field ->
                                    try {
                                        field.isAccessible = true
                                        field.name to field.get(value)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                .toMap()

                            if (properties.isNotEmpty()) {
                                dumpVariables(properties, sb, "", level + 1, maxDepth)
                            }
                        } catch (e: Exception) {
                            sb.appendLine("$indent  (unable to inspect properties)")
                        }
                    }
                }
            }
        }
    }

    private fun summarizeValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> if (value.length > 50) "\"${value.take(50)}...\"" else "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> "[${value.size} items]"
            is Map<*, *> -> "{${value.size} entries}"
            else -> "[${value::class.simpleName}]"
        }
    }
}
