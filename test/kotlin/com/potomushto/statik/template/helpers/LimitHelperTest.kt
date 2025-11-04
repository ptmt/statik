package com.potomushto.statik.template.helpers

import com.potomushto.statik.template.HandlebarsTemplateEngine
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LimitHelperTest {

    private val engine = HandlebarsTemplateEngine(Path.of("."))

    @Test
    fun `limit helper returns first N items from list`() {
        val template = "{{#each (limit items 2)}}{{this}}{{/each}}"
        val data = mapOf("items" to listOf("a", "b", "c", "d", "e"))

        val result = engine.render(template, data)

        assertEquals("ab", result)
    }

    @Test
    fun `limit helper works with empty list`() {
        val template = "{{#each (limit items 5)}}{{this}}{{/each}}"
        val data = mapOf("items" to emptyList<String>())

        val result = engine.render(template, data)

        assertEquals("", result)
    }

    @Test
    fun `limit helper returns entire list when limit is greater than size`() {
        val template = "{{#each (limit items 10)}}{{this}},{{/each}}"
        val data = mapOf("items" to listOf("one", "two", "three"))

        val result = engine.render(template, data)

        assertEquals("one,two,three,", result)
    }

    @Test
    fun `limit helper with zero returns empty list`() {
        val template = "{{#each (limit items 0)}}{{this}}{{/each}}"
        val data = mapOf("items" to listOf("a", "b", "c"))

        val result = engine.render(template, data)

        assertEquals("", result)
    }

    @Test
    fun `limit helper with negative value returns empty list`() {
        val template = "{{#each (limit items -5)}}{{this}}{{/each}}"
        val data = mapOf("items" to listOf("a", "b", "c"))

        val result = engine.render(template, data)

        assertEquals("", result)
    }

    @Test
    fun `limit helper works with map values`() {
        val template = "{{#each (limit items 2)}}{{this}},{{/each}}"
        val data = mapOf("items" to mapOf("x" to 1, "y" to 2, "z" to 3))

        val result = engine.render(template, data)

        // Should take first 2 values from the map
        assertTrue(result.contains(","))
    }

    @Test
    fun `limit helper with complex objects`() {
        val template = "{{#each (limit posts 3)}}{{title}} {{/each}}"
        val posts = listOf(
            mapOf("title" to "First"),
            mapOf("title" to "Second"),
            mapOf("title" to "Third"),
            mapOf("title" to "Fourth"),
            mapOf("title" to "Fifth")
        )
        val data = mapOf("posts" to posts)

        val result = engine.render(template, data)

        assertEquals("First Second Third ", result)
    }
}
