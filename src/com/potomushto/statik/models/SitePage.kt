package com.potomushto.statik.models

data class SitePage(
    val id: String,
    val title: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val outputPath: String,
    val navOrder: Int? = null
) {
    val path: String get() = outputPath
}
