package com.potomushto.statik.models

import java.time.LocalDateTime
import com.potomushto.statik.metadata.string
import com.potomushto.statik.metadata.stringList

data class BlogPost(
    val id: String,           // Unique identifier (can be filename without extension)
    val title: String,
    val date: LocalDateTime,
    val content: String,      // Markdown content or template content
    val rawHtml: String? = null, // Optional custom HTML
    val metadata: Map<String, Any?> = mapOf(), // For SEO and other metadata
    val outputPath: String,          // URL path like "2024/blog-title"
    val isTemplate: Boolean = false  // True if content is a Handlebars template
) {
    val path: String get() = outputPath//.removePrefix("../../posts")         // URL path like "2024/blog-title"

    /**
     * Get tags from metadata. Tags can be specified as comma-separated values.
     */
    val tags: List<String> get() = metadata.stringList("tags")

    /**
     * Get summary from metadata, falling back to truncated content.
     */
    val summary: String get() = metadata.string("summary") ?: content.take(160)

    /**
     * Get description from metadata, falling back to summary.
     */
    val description: String get() = metadata.string("description") ?: summary
}
