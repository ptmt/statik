package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.potomushto.statik.logging.LoggerFactory

class LimitHelperRegistrar : HandlebarsHelperRegistrar {
    private val logger = LoggerFactory.getLogger(LimitHelperRegistrar::class.java)

    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("limit", Helper<Any> { value, options ->
            val contextList = when (value) {
                is List<*> -> value
                is Map<*, *> -> value.values.toList()
                else -> {
                    logger.warn("Limit: unexpected context type, returning as-is")
                    return@Helper value
                }
            }

            if (contextList.isEmpty()) {
                return@Helper emptyList<Any>()
            }

            val limitParam = options?.params?.getOrNull(0)
            val limit = when (limitParam) {
                is Number -> limitParam.toInt()
                is String -> limitParam.toIntOrNull() ?: contextList.size
                is CharSequence -> limitParam.toString().toIntOrNull() ?: contextList.size
                else -> {
                    logger.warn("Limit: no valid limit parameter provided, returning full list")
                    return@Helper contextList
                }
            }

            if (limit < 0) {
                logger.warn("Limit: negative limit provided ($limit), returning empty list")
                return@Helper emptyList<Any>()
            }

            contextList.take(limit)
        })
    }
}
