package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.RssConfig
import com.potomushto.statik.models.BlogPost
import kotlin.io.path.*
import kotlin.test.*
import java.time.LocalDateTime

@OptIn(ExperimentalPathApi::class)
class RssGeneratorTest {

    private lateinit var tempRoot: java.nio.file.Path
    private lateinit var config: BlogConfig

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-rss-test")
        config = BlogConfig(
            siteName = "Test Blog",
            baseUrl = "https://example.com",
            description = "A test blog",
            author = "Test Author"
        )
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `generate creates RSS feed file`() {
        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "First Post",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>This is the first post content</p>",
                rawHtml = null,
                metadata = emptyMap(),
                outputPath = "posts/first-post"
            )
        )

        val generator = RssGenerator(config, tempRoot)
        generator.generate(posts)

        val feedFile = tempRoot.resolve("feed.xml")
        assertTrue(feedFile.exists(), "RSS feed file should exist")

        val content = feedFile.readText()
        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(content.contains("<rss version=\"2.0\""))
        assertTrue(content.contains("<title>Test Blog</title>"))
        assertTrue(content.contains("<link>https://example.com</link>"))
        assertTrue(content.contains("<description>A test blog</description>"))
        assertTrue(content.contains("<item>"))
        assertTrue(content.contains("<title>First Post</title>"))
    }

    @Test
    fun `generate does not create feed when disabled`() {
        val disabledConfig = config.copy(rss = RssConfig(enabled = false))
        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "First Post",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>Content</p>",
                rawHtml = null,
                metadata = emptyMap(),
                outputPath = "posts/first-post"
            )
        )

        val generator = RssGenerator(disabledConfig, tempRoot)
        generator.generate(posts)

        val feedFile = tempRoot.resolve("feed.xml")
        assertFalse(feedFile.exists(), "RSS feed file should not exist when disabled")
    }

    @Test
    fun `generate respects custom RSS config`() {
        val customConfig = config.copy(
            rss = RssConfig(
                enabled = true,
                fileName = "rss.xml",
                title = "Custom RSS Title",
                description = "Custom RSS Description",
                language = "fr-fr",
                maxItems = 2,
                includeFullContent = false
            )
        )

        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "First Post",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>First post content</p>",
                rawHtml = null,
                metadata = mapOf("description" to "First post description"),
                outputPath = "posts/first-post"
            ),
            BlogPost(
                id = "post2",
                title = "Second Post",
                date = LocalDateTime.of(2024, 1, 14, 10, 0),
                content = "<p>Second post content</p>",
                rawHtml = null,
                metadata = mapOf("description" to "Second post description"),
                outputPath = "posts/second-post"
            ),
            BlogPost(
                id = "post3",
                title = "Third Post",
                date = LocalDateTime.of(2024, 1, 13, 10, 0),
                content = "<p>Third post content</p>",
                rawHtml = null,
                metadata = mapOf("description" to "Third post description"),
                outputPath = "posts/third-post"
            )
        )

        val generator = RssGenerator(customConfig, tempRoot)
        generator.generate(posts)

        val feedFile = tempRoot.resolve("rss.xml")
        assertTrue(feedFile.exists(), "Custom RSS feed file should exist")

        val content = feedFile.readText()
        assertTrue(content.contains("<title>Custom RSS Title</title>"))
        assertTrue(content.contains("<description>Custom RSS Description</description>"))
        assertTrue(content.contains("<language>fr-fr</language>"))
        assertTrue(content.contains("First Post"))
        assertTrue(content.contains("Second Post"))
        assertFalse(content.contains("Third Post"), "Should only include maxItems posts")
        assertFalse(content.contains("<content:encoded>"), "Should not include full content when disabled")
    }

    @Test
    fun `generate includes full content when enabled`() {
        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "Post with Content",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>Full post content with <strong>HTML</strong></p>",
                rawHtml = null,
                metadata = emptyMap(),
                outputPath = "posts/post-with-content"
            )
        )

        val generator = RssGenerator(config, tempRoot)
        generator.generate(posts)

        val content = tempRoot.resolve("feed.xml").readText()
        assertTrue(content.contains("<content:encoded><![CDATA[<p>Full post content with <strong>HTML</strong></p>]]></content:encoded>"))
    }

    @Test
    fun `generate escapes XML special characters`() {
        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "Post with <special> & \"characters\"",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>Content</p>",
                rawHtml = null,
                metadata = mapOf("description" to "Description with & and <tags>"),
                outputPath = "posts/special-chars"
            )
        )

        val generator = RssGenerator(config, tempRoot)
        generator.generate(posts)

        val content = tempRoot.resolve("feed.xml").readText()
        assertTrue(content.contains("&lt;special&gt; &amp; &quot;characters&quot;"))
        assertTrue(content.contains("Description with &amp; and &lt;tags&gt;"))
    }

    @Test
    fun `generate uses baseUrlOverride when provided`() {
        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "Test Post",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>Content</p>",
                rawHtml = null,
                metadata = emptyMap(),
                outputPath = "posts/test-post"
            )
        )

        val generator = RssGenerator(config, tempRoot, baseUrlOverride = "http://localhost:3000")
        generator.generate(posts)

        val content = tempRoot.resolve("feed.xml").readText()
        assertTrue(content.contains("<link>http://localhost:3000</link>"))
        assertTrue(content.contains("http://localhost:3000/posts/test-post"))
    }

    @Test
    fun `generate includes post author`() {
        val posts = listOf(
            BlogPost(
                id = "post1",
                title = "Test Post",
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                content = "<p>Content</p>",
                rawHtml = null,
                metadata = emptyMap(),
                outputPath = "posts/test-post"
            )
        )

        val generator = RssGenerator(config, tempRoot)
        generator.generate(posts)

        val content = tempRoot.resolve("feed.xml").readText()
        assertTrue(content.contains("<author>Test Author</author>"))
    }
}
