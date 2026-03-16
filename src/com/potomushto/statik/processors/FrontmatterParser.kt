package com.potomushto.statik.processors

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

internal object FrontmatterParser {
    private val frontmatterRegex = Regex("^---\\s*\\r?\\n(.*?)\\r?\\n---\\s*\\r?\\n?(.*)$", RegexOption.DOT_MATCHES_ALL)
    private val yaml = Yaml(SafeConstructor(LoaderOptions()))

    fun extract(content: String): ParsedPost {
        val document = extractDocument(content)
        return ParsedPost(document.body, document.metadata)
    }

    fun extractDocument(content: String): FrontmatterDocument {
        val match = frontmatterRegex.find(content) ?: return FrontmatterDocument("", content, emptyMap())
        val yamlContent = match.groupValues[1]
        val bodyContent = match.groupValues[2]
        return FrontmatterDocument(
            frontmatter = yamlContent,
            body = bodyContent,
            metadata = parseYaml(yamlContent)
        )
    }

    fun serialize(frontmatter: String, body: String): String {
        if (frontmatter.isBlank()) {
            return body
        }
        val normalizedFrontmatter = frontmatter.trimEnd()
        val normalizedBody = body.removePrefix("\n")
        return buildString {
            appendLine("---")
            appendLine(normalizedFrontmatter)
            appendLine("---")
            append(normalizedBody)
        }
    }

    private fun parseYaml(yamlContent: String): Map<String, Any?> {
        if (yamlContent.isBlank()) return emptyMap()
        val loaded = try {
            yaml.load<Any?>(yamlContent)
        } catch (e: Exception) {
            System.err.println("YAML parsing error: ${e.message}")
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
            is Date -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                value.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime()
            )
            is Map<*, *> -> normalizeMap(value)
            is List<*> -> value.map { item -> normalizeValue(item) }
            else -> value.toString()
        }
    }
}

class FrontmatterDocument(
    val frontmatter: String,
    val body: String,
    val metadata: Map<String, Any?>
)
