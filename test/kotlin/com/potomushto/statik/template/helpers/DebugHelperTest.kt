package com.potomushto.statik.template.helpers

import com.potomushto.statik.template.HandlebarsTemplateEngine
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertContains

class DebugHelperTest {
    @Test
    fun `debug helper should dump all variables`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = """
            <h1>Test Page</h1>
            {{debug}}
        """.trimIndent()

        val data = mapOf(
            "title" to "My Post",
            "post" to mapOf(
                "title" to "Post Title",
                "date" to "2025-11-22",
                "tags" to listOf("kotlin", "handlebars", "debug")
            ),
            "count" to 42,
            "enabled" to true
        )

        val result = engine.render(template, data)

        // Verify that the debug output contains our variables
        assertContains(result, "<strong>Debug: Template Variables</strong>")
        assertContains(result, "title: \"My Post\"")
        assertContains(result, "count: 42")
        assertContains(result, "enabled: true")
        assertContains(result, "post:")
    }

    @Test
    fun `debug helper should show nested properties`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = "{{debug}}"

        val data = mapOf(
            "post" to mapOf(
                "title" to "Nested Post",
                "author" to mapOf(
                    "name" to "John Doe",
                    "email" to "john@example.com"
                )
            )
        )

        val result = engine.render(template, data)

        // Verify nested structure is shown
        assertContains(result, "post:")
        assertContains(result, "title: \"Nested Post\"")
        assertContains(result, "author:")
        assertContains(result, "name: \"John Doe\"")
        assertContains(result, "email: \"john@example.com\"")
    }

    @Test
    fun `debug helper should show list items`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = "{{debug}}"

        val data = mapOf(
            "posts" to listOf(
                mapOf("title" to "Post 1", "id" to 1),
                mapOf("title" to "Post 2", "id" to 2),
                mapOf("title" to "Post 3", "id" to 3)
            )
        )

        val result = engine.render(template, data)

        // Verify list is shown with items
        assertContains(result, "posts: [3 items]")
        assertContains(result, "[0]:")
        assertContains(result, "title: \"Post 1\"")
    }
}
