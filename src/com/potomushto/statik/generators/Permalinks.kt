package com.potomushto.statik.generators

import java.nio.file.Paths

internal fun normalizePermalinkPath(value: String?): String? {
    val trimmed = value
        ?.trim()
        ?.replace('\\', '/')
        ?.removePrefix("/")
        ?.removeSuffix("/")
        ?: return null

    if (trimmed.isBlank()) {
        return null
    }

    val normalized = Paths.get(trimmed).normalize().toString().replace('\\', '/')
    require(!normalized.startsWith("../") && normalized != ".." && !Paths.get(trimmed).isAbsolute) {
        "Permalink escapes the site output root: $value"
    }
    return normalized.takeIf { it.isNotBlank() }
}
