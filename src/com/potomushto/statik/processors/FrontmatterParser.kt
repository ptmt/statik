package com.potomushto.statik.processors

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

internal object FrontmatterParser {
    private val frontmatterRegex = Regex("^---\\s*\\r?\\n(.*?)\\r?\\n---\\s*\\r?\\n?(.*)$", RegexOption.DOT_MATCHES_ALL)
    private val yaml = Yaml(SafeConstructor(LoaderOptions()))

    fun extract(content: String): ParsedPost {
        val match = frontmatterRegex.find(content) ?: return ParsedPost(content, emptyMap())
        val yamlContent = match.groupValues[1]
        val bodyContent = match.groupValues[2]
        return ParsedPost(bodyContent, parseYaml(yamlContent))
    }

    private fun parseYaml(yamlContent: String): Map<String, Any?> {
        if (yamlContent.isBlank()) return emptyMap()
        val loaded = try {
            yaml.load<Any?>(yamlContent)
        } catch (_: Exception) {
            return emptyMap()
        }
        val root = loaded as? Map<*, *> ?: return emptyMap()
        return normalizeMap(root)
    }

    private fun normalizeMap(map: Map<*, *>): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        map.forEach { (key, value) ->
            val keyString = key?.toString()?.trim() ?: return@forEach
            result[keyString] = normalizeValue(value)
        }
        return result
    }

    private fun normalizeValue(value: Any?): Any? {
        return when (value) {
            null -> ""
            is String -> value.trim()
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> normalizeMap(value)
            is List<*> -> value.map { item -> normalizeValue(item) }
            else -> value.toString()
        }
    }
}
