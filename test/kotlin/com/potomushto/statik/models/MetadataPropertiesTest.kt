package com.potomushto.statik.models

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataPropertiesTest {

    @Test
    fun `BlogPost summary from metadata overrides content`() {
        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "This is a very long content that would normally be truncated to 160 characters for the summary but we have a custom summary in metadata",
            metadata = mapOf("summary" to "Custom summary from metadata"),
            outputPath = "test"
        )

        assertEquals("Custom summary from metadata", post.summary)
    }

    @Test
    fun `BlogPost summary falls back to truncated content`() {
        val longContent = "a".repeat(200)
        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = longContent,
            metadata = emptyMap(),
            outputPath = "test"
        )

        assertEquals(longContent.take(160), post.summary)
        assertEquals(160, post.summary.length)
    }

    @Test
    fun `BlogPost description uses metadata description first, then summary`() {
        val post1 = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf(
                "description" to "Custom description",
                "summary" to "Custom summary"
            ),
            outputPath = "test"
        )

        assertEquals("Custom description", post1.description)

        val post2 = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf("summary" to "Custom summary"),
            outputPath = "test"
        )

        assertEquals("Custom summary", post2.description)
    }

    @Test
    fun `SitePage summary from metadata`() {
        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("summary" to "Custom page summary"),
            outputPath = "test"
        )

        assertEquals("Custom page summary", page.summary)
    }

    @Test
    fun `SitePage summary is null when not in metadata`() {
        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = emptyMap(),
            outputPath = "test"
        )

        assertNull(page.summary)
    }

    @Test
    fun `SitePage description uses metadata description first, then summary`() {
        val page1 = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf(
                "description" to "Custom description",
                "summary" to "Custom summary"
            ),
            outputPath = "test"
        )

        assertEquals("Custom description", page1.description)

        val page2 = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("summary" to "Custom summary"),
            outputPath = "test"
        )

        assertEquals("Custom summary", page2.description)
    }

    @Test
    fun `tags property parses comma-separated values`() {
        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf("tags" to "kotlin, web, tutorial"),
            outputPath = "test"
        )

        assertEquals(listOf("kotlin", "web", "tutorial"), post.tags)

        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("tags" to "kotlin, web, tutorial"),
            outputPath = "test"
        )

        assertEquals(listOf("kotlin", "web", "tutorial"), page.tags)
    }

    @Test
    fun `tags property handles empty and whitespace`() {
        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf("tags" to "kotlin,  , web,, tutorial, "),
            outputPath = "test"
        )

        // Should filter out empty strings and trim whitespace
        assertEquals(listOf("kotlin", "web", "tutorial"), post.tags)
    }
}
