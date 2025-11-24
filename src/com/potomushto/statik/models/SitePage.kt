package com.potomushto.statik.models

data class SitePage(
    val id: String,
    val title: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val outputPath: String,
    val navOrder: Int? = null,
    val isTemplate: Boolean = false  // True if content is a Handlebars template
) {
    val path: String get() = outputPath

    /**
     * Get tags from metadata. Tags can be specified as comma-separated values.
     */
    val tags: List<String> get() = metadata["tags"]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    /**
     * Get summary from metadata. Returns null if not set.
     */
    val summary: String? get() = metadata["summary"]

    /**
     * Get description from metadata, falling back to summary. Returns null if neither is set.
     */
    val description: String? get() = metadata["description"] ?: summary
}
