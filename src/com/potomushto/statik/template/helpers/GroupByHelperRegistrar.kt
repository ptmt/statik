package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.potomushto.statik.logging.LoggerFactory

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

            val grouped = contextList.groupBy { item ->
                val metadata = when (item) {
                    is Map<*, *> -> item["metadata"] as? Map<*, *>
                    else -> {
                        try {
                            val metadataField = item?.javaClass?.getMethod("getMetadata")
                            metadataField?.invoke(item) as? Map<*, *>
                        } catch (e: Exception) {
                            logger.debug("GroupBy: failed to read metadata via reflection", e)
                            null
                        }
                    }
                }
                metadata?.get(key) as? String ?: ""
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
}
