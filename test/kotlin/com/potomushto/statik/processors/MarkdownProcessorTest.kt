package com.potomushto.statik.processors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun `metadata for different documents stays independent`() {
        val first = """
            ---
            title: First Post
            description: First description
            ---
            # Hello
        """.trimIndent()

        val second = """
            ---
            title: Second Post
            ---
            # Hello again
        """.trimIndent()

        processor.process(first)
        val secondMetadata = processor.process(second).metadata

        assertNull(
            secondMetadata["description"],
            "Second markdown file should not inherit description from previous one"
        )
    }

    @Test
    fun `image with title renders as figure with figcaption`() {
        val source = """
            ![Alt text](image.jpg "This is a caption")
        """.trimIndent()

        val result = processor.process(source)

        assertTrue(result.content.contains("<figure>"), "Should contain figure tag")
        assertTrue(result.content.contains("<img"), "Should contain img tag")
        assertTrue(result.content.contains("image.jpg"), "Should have correct src")
        assertTrue(result.content.contains("Alt text"), "Should have correct alt")
        assertTrue(result.content.contains("<figcaption>"), "Should contain figcaption tag")
        assertTrue(result.content.contains("This is a caption"), "Should contain caption text")
        assertTrue(result.content.contains("</figcaption>"), "Should close figcaption")
        assertTrue(result.content.contains("</figure>"), "Should close figure")
    }

    @Test
    fun `image without title renders as plain img`() {
        val source = """
            ![Alt text](image.jpg)
        """.trimIndent()

        val result = processor.process(source)

        assertTrue(result.content.contains("<img"), "Should contain img tag")
        assertTrue(result.content.contains("image.jpg"), "Should have correct src")
        assertTrue(result.content.contains("Alt text"), "Should have correct alt")
        assertTrue(!result.content.contains("<figure>"), "Should not contain figure tag")
        assertTrue(!result.content.contains("<figcaption>"), "Should not contain figcaption tag")
    }

    @Test
    fun `image with empty title renders as plain img`() {
        val source = """
            ![Alt text](image.jpg "")
        """.trimIndent()

        val result = processor.process(source)

        assertTrue(result.content.contains("<img"), "Should contain img tag")
        assertTrue(!result.content.contains("<figure>"), "Should not wrap in figure for empty title")
        assertTrue(!result.content.contains("<figcaption>"), "Should not create figcaption for empty title")
    }

    @Test
    fun `multiple images with mixed caption presence`() {
        val source = """
            ![First](img1.jpg "Caption 1")

            ![Second](img2.jpg)

            ![Third](img3.jpg "Caption 3")
        """.trimIndent()

        val result = processor.process(source)

        // First image should have figure
        assertTrue(result.content.contains("Caption 1"))

        // Second image shouldn't have figure (checked by counting figure tags)
        val figureCount = result.content.split("<figure>").size - 1
        assertEquals(2, figureCount, "Should have exactly 2 figure tags")

        // Third image should have figure
        assertTrue(result.content.contains("Caption 3"))
    }
}
