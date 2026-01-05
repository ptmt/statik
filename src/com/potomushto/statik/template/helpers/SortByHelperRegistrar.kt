package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.metadata.metadataValueAsString

class SortByHelperRegistrar : HandlebarsHelperRegistrar {
    private val logger = LoggerFactory.getLogger(SortByHelperRegistrar::class.java)

    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("sortBy", Helper<Any> { value, options ->
            val contextList = when (value) {
                is List<*> -> value
                is Map<*, *> -> value.values.toList()
                else -> {
                    logger.warn("SortBy: unexpected context type, returning as-is")
                    return@Helper value
                }
            }

            if (contextList.isEmpty()) {
                return@Helper emptyList<Any>()
            }

            val keyParam = options?.params?.getOrNull(0)
            val key = when (keyParam) {
                is String -> keyParam
                is CharSequence -> keyParam.toString()
                else -> return@Helper contextList
            }

            contextList.sortedBy { item ->
                when (item) {
                    is Map<*, *> -> {
                        val metadata = item["metadata"] as? Map<*, *>
                        val metadataValue = metadata?.get(key)
                        when (metadataValue) {
                            is Number -> metadataValue.toInt()
                            else -> metadataValueAsString(metadataValue)?.toIntOrNull() ?: 0
                        }
                    }
                    else -> 0
                }
            }
        })
    }
}
