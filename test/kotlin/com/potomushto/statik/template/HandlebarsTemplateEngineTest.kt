package com.potomushto.statik.template

import com.github.jknack.handlebars.Helper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

class HandlebarsTemplateEngineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var engine: HandlebarsTemplateEngine

    @BeforeEach
    fun setUp() {
        engine = HandlebarsTemplateEngine(tempDir)
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
}
