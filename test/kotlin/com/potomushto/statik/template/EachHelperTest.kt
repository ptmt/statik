package com.potomushto.statik.template

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EachHelperTest {

    data class PostWithTags(
        val title: String,
        val tags: List<String>
    )

    @Test
    fun `each helper should iterate over list items only`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = """
            Tags:
            {{#each tags}}
            {{this}}
            {{/each}}
        """.trimIndent()

        val data = mapOf(
            "tags" to listOf("kotlin", "web", "tutorial")
        )

        val result = engine.render(template, data)

        // Should only contain the actual tag values
        assert(result.contains("kotlin"))
        assert(result.contains("web"))
        assert(result.contains("tutorial"))

        // Should NOT contain Java/Kotlin List methods
        assertFalse(result.contains("isEmpty"))
        assertFalse(result.contains("hashCode"))
        assertFalse(result.contains("size"))
    }

    @Test
    fun `each helper should iterate over property that returns list`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = """
            Tags:
            {{#each post.tags}}
            {{this}}
            {{/each}}
        """.trimIndent()

        val post = PostWithTags(
            title = "My Post",
            tags = listOf("kotlin", "web", "tutorial")
        )

        val data = mapOf(
            "post" to post
        )

        val result = engine.render(template, data)

        // Should only contain the actual tag values
        assert(result.contains("kotlin"))
        assert(result.contains("web"))
        assert(result.contains("tutorial"))

        // Should NOT contain Java/Kotlin List methods
        assertFalse(result.contains("isEmpty"))
        assertFalse(result.contains("hashCode"))
        assertFalse(result.contains("size"))
    }

    @Test
    fun `each helper with nested objects should work correctly`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = """
            {{#each items}}
            {{name}}: {{value}}
            {{/each}}
        """.trimIndent()

        val data = mapOf(
            "items" to listOf(
                mapOf("name" to "foo", "value" to "bar"),
                mapOf("name" to "baz", "value" to "qux")
            )
        )

        val result = engine.render(template, data)

        assert(result.contains("foo: bar"))
        assert(result.contains("baz: qux"))
    }
}
