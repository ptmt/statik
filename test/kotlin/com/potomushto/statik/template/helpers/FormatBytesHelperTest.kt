package com.potomushto.statik.template.helpers

import com.potomushto.statik.template.HandlebarsTemplateEngine
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatBytesHelperTest {

    private val engine = HandlebarsTemplateEngine(Path.of("."))

    @Test
    fun `formatBytes helper formats bytes`() {
        val result = engine.render("{{formatBytes size}}", mapOf("size" to 512))

        assertEquals("512 B", result)
    }

    @Test
    fun `formatBytes helper formats kilobytes`() {
        val result = engine.render("{{formatBytes size}}", mapOf("size" to 1536))

        assertEquals("1.5 KB", result)
    }

    @Test
    fun `formatBytes helper formats megabytes`() {
        val result = engine.render("{{formatBytes size}}", mapOf("size" to 2 * 1024 * 1024))

        assertEquals("2 MB", result)
    }

    @Test
    fun `formatBytes helper returns empty string for missing size`() {
        val result = engine.render("{{formatBytes size}}", emptyMap())

        assertEquals("", result)
    }
}
