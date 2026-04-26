package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.metadata.metadataValueAsString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class GroupByHelperRegistrar : HandlebarsHelperRegistrar {
    private val logger = LoggerFactory.getLogger(GroupByHelperRegistrar::class.java)

    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("groupBy", Helper<Any> { value, options ->
            logger.debug { "GroupBy called with context type: ${value?.javaClass?.name}" }

            val contextList = when (value) {
                is List<*> -> value
                else -> {
                    logger.warn("GroupBy: unexpected context type")
                    return@Helper emptyList<Map<String, Any?>>()
                }
            }

            if (contextList.isEmpty()) {
                logger.debug { "GroupBy: empty list" }
                return@Helper emptyList<Map<String, Any?>>()
            }

            val keyParam = options?.params?.getOrNull(0)
            val key = when (keyParam) {
                is String -> keyParam
                is CharSequence -> keyParam.toString()
                else -> return@Helper contextList
            }
            val format = options?.hash?.get("format")?.toString()

            val grouped = contextList.groupBy { item ->
                groupValueAsString(resolveGroupValue(item, key), format)
            }

            logger.debug { "GroupBy grouped into ${grouped.size} groups: ${grouped.keys}" }

            val categoryOrder = mapOf(
                "core" to 1,
                "theme" to 2,
                "paths" to 3,
                "devServer" to 4,
                "staticDatasource" to 5,
                "rss" to 6
            )

            grouped.entries
                .sortedBy { categoryOrder[it.key] ?: 999 }
                .map { (groupName, items) ->
                    mapOf(
                        "name" to groupName,
                        "items" to items
                    )
                }
        })
    }

    private fun resolveGroupValue(item: Any?, keyPath: String): Any? {
        if (item == null) return null

        val normalizedKeyPath = keyPath.trim()
        if (normalizedKeyPath.isEmpty()) return null

        return if (normalizedKeyPath.contains('.')) {
            resolvePath(item, normalizedKeyPath)
        } else {
            resolveTopLevelValue(item, normalizedKeyPath) ?: resolveMetadataValue(item, normalizedKeyPath)
        }
    }

    private fun resolvePath(item: Any, keyPath: String): Any? {
        return keyPath.split('.').fold(item as Any?) { current, segment ->
            when (current) {
                null -> null
                is Map<*, *> -> current[segment]
                else -> readProperty(current, segment)
            }
        }
    }

    private fun resolveTopLevelValue(item: Any, key: String): Any? {
        return when (item) {
            is Map<*, *> -> item[key]
            else -> readProperty(item, key)
        }
    }

    private fun resolveMetadataValue(item: Any, key: String): Any? {
        val metadata = when (item) {
            is Map<*, *> -> item["metadata"] as? Map<*, *>
            else -> readProperty(item, "metadata") as? Map<*, *>
        }

        return metadata?.get(key)
    }

    private fun readProperty(item: Any, propertyName: String): Any? {
        val capitalized = propertyName.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
        }
        val getterNames = listOf("get$capitalized", "is$capitalized")

        return getterNames.asSequence()
            .mapNotNull { getterName ->
                item.javaClass.methods
                    .firstOrNull { it.name == getterName && it.parameterCount == 0 }
            }
            .mapNotNull { method ->
                runCatching { method.invoke(item) }
                    .onFailure { error ->
                        logger.debug("GroupBy: failed to invoke ${method.name} on ${item.javaClass.simpleName}", error)
                    }
                    .getOrNull()
            }
            .firstOrNull()
    }

    private fun groupValueAsString(value: Any?, format: String?): String {
        if (value == null) return ""
        if (!format.isNullOrBlank()) {
            val formatter = DateTimeFormatter.ofPattern(format)
            return when (value) {
                is LocalDateTime -> value.format(formatter)
                is LocalDate -> value.format(formatter)
                else -> metadataValueAsString(value) ?: ""
            }
        }

        return metadataValueAsString(value) ?: ""
    }
}
