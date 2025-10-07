package com.potomushto.statik.processors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownProcessorTest {

    private val processor = MarkdownProcessor()

    @Test
    fun `process extracts metadata and renders html`() {
        val source = """
            ---
            title: "Hello World"
            published: 2024-06-05T10:00:00
            summary: ' value '
            ---
            # Heading

            Paragraph with [link](https://example.com).
        """.trimIndent()

        val result = processor.process(source)

        assertEquals("Hello World", result.metadata["title"])
        assertEquals("2024-06-05T10:00:00", result.metadata["published"])
        assertEquals("value", result.metadata["summary"])

        assertTrue(result.content.contains("<h1"))
        assertTrue(result.content.contains("Heading"))
        assertTrue(result.content.contains("<a"))
        assertTrue(result.content.contains("https://example.com"))
    }

    @Test
    fun `process handles documents without front matter`() {
        val source = """
            ## Plain Markdown

            Text without metadata.
        """.trimIndent()

        val result = processor.process(source)

        assertEquals(emptyMap(), result.metadata)
        assertTrue(result.content.contains("<h2"))
        assertTrue(result.content.contains("Plain Markdown"))
        assertTrue(result.content.contains("<p"))
        assertTrue(result.content.contains("Text without metadata."))
    }
}
