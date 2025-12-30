package com.potomushto.statik.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

class HandlebarsTemplateEngineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var templatesDir: Path
    private lateinit var engine: HandlebarsTemplateEngine

    @BeforeEach
    fun setUp() {
        templatesDir = tempDir.resolve("templates")
        Files.createDirectories(templatesDir)
        engine = HandlebarsTemplateEngine(templatesDir)
    }

    @Test
    fun `substring helper extracts substring with start and end indices`() {
        val template = "{{substring name 0 5}}"
        val data = mapOf("name" to "John Doe")
        val expected = "John "
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper extracts substring from start to end of string`() {
        val template = "{{substring name 5}}"
        val data = mapOf("name" to "John Doe")
        val expected = "Doe"
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper handles out of bounds indices`() {
        val template = "{{substring name 0 20}}"
        val data = mapOf("name" to "John Doe")
        val expected = "John Doe"
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper handles null context`() {
        val template = "{{substring name 0 5}}"
        val data = mapOf("name" to null)
        val expected = ""
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper returns empty string when start is greater than end`() {
        val template = "{{substring name 5 0}}"
        val data = mapOf("name" to "John Doe")
        val expected = ""
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper works with empty string`() {
        val template = "{{substring name 0 5}}"
        val data = mapOf("name" to "")
        val expected = ""
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper handles negative indices gracefully`() {
        val template = "{{substring name -2 5}}"
        val data = mapOf("name" to "John Doe")
        val expected = "John "
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `substring helper extracts first character`() {
        val template = "{{substring name 0 1}}"
        val data = mapOf("name" to "John Doe")
        val expected = "J"
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `eq helper compares equal strings`() {
        val template = "{{#if (eq lang \"en\")}}English{{else}}Other{{/if}}"
        val data = mapOf("lang" to "en")
        val expected = "English"
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `eq helper compares unequal strings`() {
        val template = "{{#if (eq lang \"en\")}}English{{else}}Other{{/if}}"
        val data = mapOf("lang" to "fr")
        val expected = "Other"
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `eq helper with nested metadata access`() {
        val template = "{{#if (eq metadata.lang \"en\")}}English{{else}}Other{{/if}}"
        val data = mapOf("metadata" to mapOf("lang" to "en"))
        val expected = "English"
        val actual = engine.render(template, data)
        assertEquals(expected, actual)
    }

    @Test
    fun `content helper injects overrides into layout blocks`() {
        val layout = templatesDir.resolve("layouts/default.hbs")
        Files.createDirectories(layout.parent)
        Files.writeString(
            layout,
            """
            <html>
              <head>{{#block "head"}}<meta charset="utf-8">{{/block}}</head>
              <body>
                {{{content}}}
                {{#block "scripts"}}{{/block}}
              </body>
            </html>
            """.trimIndent()
        )

        val template = """
            {{#content "head"}}<title>{{title}}</title>{{/content}}
            <main>{{body}}</main>
            {{#content "scripts"}}<script src="/app.js"></script>{{/content}}
        """.trimIndent()

        val html = engine.renderWithLayout(template, mapOf(
            "layout" to "default",
            "title" to "Hello",
            "body" to "Content"
        ))

        val normalizedHtml = html.normalizeQuotes()
        assertTrue(normalizedHtml.contains("<title>Hello</title>"))
        assertTrue(normalizedHtml.contains("<script src=\"/app.js\"></script>"))
        assertTrue(normalizedHtml.contains("<main>Content</main>"))
        assertFalse(normalizedHtml.contains("<meta charset=\"utf-8\">"))
    }

    @Test
    fun `block helper falls back to default content when not overridden`() {
        val layout = templatesDir.resolve("layouts/default.hbs")
        Files.createDirectories(layout.parent)
        Files.writeString(
            layout,
            """
            <html>
              <head>{{#block "head"}}<meta charset="utf-8">{{/block}}</head>
              <body>{{{content}}}</body>
            </html>
            """.trimIndent()
        )

        val template = """
            <main>{{body}}</main>
        """.trimIndent()

        val html = engine.renderWithLayout(template, mapOf(
            "layout" to "default",
            "body" to "Fallback"
        ))

        val normalizedHtml = html.normalizeQuotes()
        assertTrue(normalizedHtml.contains("<meta charset=\"utf-8\">"))
        assertTrue(normalizedHtml.contains("<main>Fallback</main>"))
    }

    @Test
    fun `block helper allows overriding with empty content`() {
        val layout = templatesDir.resolve("layouts/default.hbs")
        Files.createDirectories(layout.parent)
        Files.writeString(
            layout,
            """
            <html>
              <body>
                {{{content}}}
                {{#block "footer"}}<footer>Default footer</footer>{{/block}}
              </body>
            </html>
            """.trimIndent()
        )

        val template = """
            <main>{{body}}</main>
            {{#content "footer"}}{{/content}}
        """.trimIndent()

        val html = engine.renderWithLayout(template, mapOf(
            "layout" to "default",
            "body" to "Page body"
        ))

        val normalizedHtml = html.normalizeQuotes()
        assertTrue(normalizedHtml.contains("<main>Page body</main>"))
        assertFalse(normalizedHtml.contains("Default footer"))
    }

    private fun String.normalizeQuotes(): String = this.replace("\\\"", "\"")
}
