package com.potomushto.statik.processors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownProcessorTest {
    private val processor = MarkdownProcessor()
    private val htmlPostProcessor = HtmlPostProcessor()

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
        assertTrue(result.content.contains("title=\"This is a caption\""), "Should preserve title attribute for datasource")
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

    @Test
    fun `blockquote with emdash attribution renders as figure with figcaption`() {
        val source = """
            > Üben macht den Meister.
            > — German Proverb
        """.trimIndent()

        val result = processor.process(source)

        assertTrue(result.content.contains("<figure>"), "Should contain figure tag")
        assertTrue(result.content.contains("<blockquote"), "Should contain blockquote tag")
        assertTrue(result.content.contains("data-author=\"German Proverb\""), "Should have data-author attribute")
        assertTrue(result.content.contains("Üben macht den Meister."), "Should contain quote text")
        assertTrue(!result.content.contains("— German Proverb</p>"), "Should not render attribution in blockquote")
        assertTrue(result.content.contains("<figcaption>"), "Should contain figcaption tag")
        assertTrue(result.content.contains("— German Proverb"), "Should contain attribution in figcaption")
        assertTrue(result.content.contains("</figcaption>"), "Should close figcaption")
        assertTrue(result.content.contains("</figure>"), "Should close figure")
    }

    @Test
    fun `blockquote with double hyphen attribution renders as figure with figcaption`() {
        val source = """
            > Practice makes perfect.
            > -- English Proverb
        """.trimIndent()

        val result = processor.process(source)

        assertTrue(result.content.contains("<figure>"), "Should contain figure tag")
        assertTrue(result.content.contains("data-author=\"English Proverb\""), "Should have data-author attribute")
        assertTrue(result.content.contains("Practice makes perfect."), "Should contain quote text")
        assertTrue(result.content.contains("<figcaption>"), "Should contain figcaption tag")
        assertTrue(result.content.contains("— English Proverb"), "Should contain attribution in figcaption")
    }

    @Test
    fun `blockquote without attribution renders as plain blockquote`() {
        val source = """
            > This is just a regular quote.
            > No attribution here.
        """.trimIndent()

        val result = processor.process(source)

        assertTrue(result.content.contains("<blockquote"), "Should contain blockquote tag")
        assertTrue(result.content.contains("This is just a regular quote."), "Should contain quote text")
        assertTrue(!result.content.contains("<figure>"), "Should not contain figure tag")
        assertTrue(!result.content.contains("<figcaption>"), "Should not contain figcaption tag")
        assertTrue(!result.content.contains("data-author"), "Should not have data-author attribute")
    }

    @Test
    fun `html blockquote with data-author gets wrapped in figure`() {
        val html = """
            <blockquote data-collect="quotes" data-author="Ada Lovelace">
              That brain of mine is more than merely mortal.
            </blockquote>
        """.trimIndent()

        val result = htmlPostProcessor.process(html)

        assertTrue(result.contains("<figure>"), "Should contain figure tag")
        assertTrue(result.contains("<blockquote"), "Should contain blockquote tag")
        assertTrue(result.contains("data-author=\"Ada Lovelace\""), "Should preserve data-author attribute")
        assertTrue(result.contains("data-collect=\"quotes\""), "Should preserve data-collect attribute")
        assertTrue(result.contains("That brain of mine is more than merely mortal."), "Should contain quote text")
        assertTrue(result.contains("<figcaption>"), "Should contain figcaption tag")
        assertTrue(result.contains("— Ada Lovelace"), "Should contain attribution in figcaption")
        assertTrue(result.contains("</figcaption>"), "Should close figcaption")
        assertTrue(result.contains("</figure>"), "Should close figure")
    }

    @Test
    fun `html blockquote without data-author stays unchanged`() {
        val html = """
            <blockquote>
              Just a regular quote.
            </blockquote>
        """.trimIndent()

        val result = htmlPostProcessor.process(html)

        assertTrue(result.contains("<blockquote"), "Should contain blockquote tag")
        assertTrue(result.contains("Just a regular quote."), "Should contain quote text")
        assertTrue(!result.contains("<figure>"), "Should not contain figure tag")
        assertTrue(!result.contains("<figcaption>"), "Should not contain figcaption tag")
    }
}
