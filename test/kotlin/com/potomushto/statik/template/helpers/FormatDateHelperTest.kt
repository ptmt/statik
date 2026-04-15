package com.potomushto.statik.template.helpers

import com.potomushto.statik.template.HandlebarsTemplateEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import java.nio.file.Path
import java.time.LocalDateTime

class FormatDateHelperTest {

    private val engine = HandlebarsTemplateEngine(Path.of("."))
    private val date = LocalDateTime.of(2024, 10, 7, 15, 30)

    @Test
    fun `formatDate helper supports positional format string`() {
        val template = "{{formatDate date \"yyyy\"}}"

        val result = engine.render(template, mapOf("date" to date))

        assertEquals("2024", result)
    }

    @Test
    fun `formatDate helper supports named format string`() {
        val template = "{{formatDate date format=\"yyyy-MM-dd\"}}"

        val result = engine.render(template, mapOf("date" to date))

        assertEquals("2024-10-07", result)
    }
}
