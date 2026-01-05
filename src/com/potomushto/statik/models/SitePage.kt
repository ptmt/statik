package com.potomushto.statik.models

import com.potomushto.statik.metadata.string
import com.potomushto.statik.metadata.stringList

data class SitePage(
    val id: String,
    val title: String,
    val content: String,
    val metadata: Map<String, Any?> = emptyMap(),
    val outputPath: String,
    val navOrder: Int? = null,
    val isTemplate: Boolean = false  // True if content is a Handlebars template
) {
    val path: String get() = outputPath

    /**
     * Get tags from metadata. Tags can be specified as comma-separated values.
     */
    val tags: List<String> get() = metadata.stringList("tags")

    /**
     * Get summary from metadata. Returns null if not set.
     */
    val summary: String? get() = metadata.string("summary")

    /**
     * Get description from metadata, falling back to summary. Returns null if neither is set.
     */
    val description: String? get() = metadata.string("description") ?: summary
}
