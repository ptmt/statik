package com.potomushto.statik.template

/**
 * Provides minimal fallback templates when user templates are missing.
 * Templates are content-only and use the default layout.
 *
 * Templates are loaded from resources/fallback-templates/ directory.
 */
object FallbackTemplates {

    /**
     * Load a template from resources
     */
    private fun loadTemplate(name: String): String {
        val resourcePath = "/fallback-templates/$name.hbs"
        return FallbackTemplates::class.java.getResourceAsStream(resourcePath)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Fallback template not found: $resourcePath")
    }

    /**
     * Load a layout template from resources
     */
    private fun loadLayout(name: String): String {
        val resourcePath = "/fallback-templates/layouts/$name.hbs"
        return FallbackTemplates::class.java.getResourceAsStream(resourcePath)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Fallback layout not found: $resourcePath")
    }

    val HOME_TEMPLATE: String by lazy { loadTemplate("home") }

    val POST_TEMPLATE: String by lazy { loadTemplate("post") }

    val PAGE_TEMPLATE: String by lazy { loadTemplate("page") }

    val YEAR_TEMPLATE: String by lazy { loadTemplate("year") }

    val POSTS_TEMPLATE: String by lazy { loadTemplate("posts") }

    val DEFAULT_LAYOUT: String by lazy { loadLayout("default") }
}