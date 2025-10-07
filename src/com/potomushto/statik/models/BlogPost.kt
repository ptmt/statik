package com.potomushto.statik.models

import java.time.LocalDateTime

data class BlogPost(
    val id: String,           // Unique identifier (can be filename without extension)
    val title: String,
    val date: LocalDateTime,
    val content: String,      // Markdown content or template content
    val rawHtml: String? = null, // Optional custom HTML
    val metadata: Map<String, String> = mapOf(), // For SEO and other metadata
    val outputPath: String,          // URL path like "2024/blog-title"
    val isTemplate: Boolean = false  // True if content is a Handlebars template
) {
    val path: String get() = outputPath//.removePrefix("../../posts")         // URL path like "2024/blog-title"
}