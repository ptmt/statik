package com.potomushto.statik.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class BlogPost(
    val id: String,           // Unique identifier (can be filename without extension)
    val title: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime,
    val content: String,      // Markdown content or template content
    val rawHtml: String? = null, // Optional custom HTML
    val metadata: Map<String, String> = mapOf(), // For SEO and other metadata
    val outputPath: String,          // URL path like "2024/blog-title"
    val isTemplate: Boolean = false  // True if content is a Handlebars template
) {
    val path: String get() = outputPath//.removePrefix("../../posts")         // URL path like "2024/blog-title"

    /**
     * Get tags from metadata. Tags can be specified as comma-separated values.
     */
    val tags: List<String> get() = metadata["tags"]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}