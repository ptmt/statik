package com.potomushto.statik.metadata

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val json = Json { prettyPrint = false }

fun metadataValueAsString(value: Any?): String? {
    return when (value) {
        null -> null
        is String -> value.trim()
        is Number -> value.toString()
        is Boolean -> value.toString()
        else -> value.toString()
    }
}

fun Map<String, Any?>.string(key: String): String? = metadataValueAsString(this[key])

fun Map<String, Any?>.stringOrEmpty(key: String): String = metadataValueAsString(this[key]) ?: ""

fun Map<String, Any?>.stringList(key: String): List<String> {
    val value = this[key]
    return when (value) {
        is List<*> -> value.mapNotNull { metadataValueAsString(it) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        else -> metadataValueAsString(value)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
}

fun Map<String, Any?>.toStringMap(): Map<String, String> {
    val result = LinkedHashMap<String, String>()
    for ((key, value) in this) {
        val stringValue = when (value) {
            null -> ""
            is String, is Number, is Boolean -> metadataValueAsString(value) ?: ""
            is Map<*, *>, is List<*> -> json.encodeToString(toJsonElement(value))
            else -> value.toString()
        }
        result[key] = stringValue
    }
    return result
}

private fun toJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> {
            val map = value.entries.associate { (k, v) ->
                k.toString() to toJsonElement(v)
            }
            JsonObject(map)
        }
        is List<*> -> JsonArray(value.map { item -> toJsonElement(item) })
        else -> JsonPrimitive(value.toString())
    }
}
