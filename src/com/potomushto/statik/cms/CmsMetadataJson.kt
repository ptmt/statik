package com.potomushto.statik.cms

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object CmsMetadataJson {
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    fun encode(metadata: Map<String, Any?>): String {
        return json.encodeToString(JsonObject.serializer(), metadata.toJsonObject())
    }

    fun decode(encoded: String): Map<String, Any?> {
        if (encoded.isBlank()) return emptyMap()
        return json.parseToJsonElement(encoded).jsonObject.toMetadataMap()
    }

    private fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
        forEach { (key, value) ->
            put(key, value.toJsonElement())
        }
    }

    private fun Map<String, JsonElement>.toMetadataMap(): Map<String, Any?> {
        return entries.associate { (key, value) -> key to value.toMetadataValue() }
    }

    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            null -> JsonNull
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Map<*, *> -> {
                buildJsonObject {
                    this@toJsonElement.forEach { (key, value) ->
                        if (key != null) {
                            put(key.toString(), value.toJsonElement())
                        }
                    }
                }
            }
            is Iterable<*> -> buildJsonArray {
                this@toJsonElement.forEach { add(it.toJsonElement()) }
            }
            is Array<*> -> buildJsonArray {
                this@toJsonElement.forEach { add(it.toJsonElement()) }
            }
            else -> JsonPrimitive(toString())
        }
    }

    private fun JsonElement.toMetadataValue(): Any? {
        return when (this) {
            JsonNull -> null
            is JsonObject -> entries.associate { (key, value) -> key to value.toMetadataValue() }
            is JsonArray -> map { it.toMetadataValue() }
            is JsonPrimitive -> jsonPrimitive.content
        }
    }
}
