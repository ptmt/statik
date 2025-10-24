package com.potomushto.statik.template

import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalPathApi::class)
class HandlebarsTemplateEngineTest {

    private lateinit var tempDir: java.nio.file.Path
    private lateinit var templateEngine: HandlebarsTemplateEngine

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("statik-handlebars-test")
        (tempDir / "layouts").createDirectories()
        templateEngine = HandlebarsTemplateEngine(tempDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `substring helper extracts substring with start and end indices`() {
        val template = "{{substring text 0 3}}"
        val result = templateEngine.render(template, mapOf("text" to "Hello World"))
        assertEquals("Hel", result)
    }

    @Test
    fun `substring helper extracts substring from start to end of string`() {
        val template = "{{substring text 6}}"
        val result = templateEngine.render(template, mapOf("text" to "Hello World"))
        assertEquals("World", result)
    }

    @Test
    fun `substring helper extracts first character`() {
        val template = "{{substring author 0 1}}"
        val result = templateEngine.render(template, mapOf("author" to "John Doe"))
        assertEquals("J", result)
    }

    @Test
    fun `substring helper handles null context`() {
        val template = "{{substring text 0 3}}"
        val result = templateEngine.render(template, mapOf("text" to null))
        assertEquals("", result)
    }

    @Test
    fun `substring helper handles out of bounds indices`() {
        val template = "{{substring text 0 100}}"
        val result = templateEngine.render(template, mapOf("text" to "Short"))
        assertEquals("Short", result)
    }

    @Test
    fun `substring helper handles negative indices gracefully`() {
        val template = "{{substring text -5 3}}"
        val result = templateEngine.render(template, mapOf("text" to "Hello"))
        assertEquals("Hel", result)
    }

    @Test
    fun `substring helper returns empty string when start is greater than end`() {
        val template = "{{substring text 5 3}}"
        val result = templateEngine.render(template, mapOf("text" to "Hello World"))
        assertEquals("", result)
    }

    @Test
    fun `substring helper works with empty string`() {
        val template = "{{substring text 0 3}}"
        val result = templateEngine.render(template, mapOf("text" to ""))
        assertEquals("", result)
    }
}
