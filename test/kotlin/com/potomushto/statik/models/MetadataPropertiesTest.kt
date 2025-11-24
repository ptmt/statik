package com.potomushto.statik.models

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataPropertiesTest {

    @Test
    fun `BlogPost description from metadata overrides content`() {
        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "This is a very long content that would normally be truncated to 160 characters for the description but we have a custom description in metadata",
            metadata = mapOf("description" to "Custom description from metadata"),
            outputPath = "test"
        )

        assertEquals("Custom description from metadata", post.description)
    }

    @Test
    fun `BlogPost description falls back to truncated content`() {
        val longContent = "a".repeat(200)
        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = longContent,
            metadata = emptyMap(),
            outputPath = "test"
        )

        assertEquals(longContent.take(160), post.description)
        assertEquals(160, post.description.length)
    }

    @Test
    fun `BlogPost summary uses metadata summary first, then description`() {
        val post1 = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf(
                "summary" to "Custom summary",
                "description" to "Custom description"
            ),
            outputPath = "test"
        )

        assertEquals("Custom summary", post1.summary)

        val post2 = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf("description" to "Custom description"),
            outputPath = "test"
        )

        assertEquals("Custom description", post2.summary)
    }

    @Test
    fun `SitePage description from metadata`() {
        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("description" to "Custom page description"),
            outputPath = "test"
        )

        assertEquals("Custom page description", page.description)
    }

    @Test
    fun `SitePage description is null when not in metadata`() {
        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = emptyMap(),
            outputPath = "test"
        )

        assertNull(page.description)
    }

    @Test
    fun `SitePage summary uses metadata summary first, then description`() {
        val page1 = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf(
                "summary" to "Custom summary",
                "description" to "Custom description"
            ),
            outputPath = "test"
        )

        assertEquals("Custom summary", page1.summary)

        val page2 = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("description" to "Custom description"),
            outputPath = "test"
        )

        assertEquals("Custom description", page2.summary)
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
