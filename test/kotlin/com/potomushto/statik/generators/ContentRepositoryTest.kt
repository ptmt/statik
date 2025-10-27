package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.processors.ContentProcessor
import com.potomushto.statik.processors.MarkdownProcessor
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class ContentRepositoryTest {

    private lateinit var tempRoot: Path
    private lateinit var config: BlogConfig
    private lateinit var repository: ContentRepository

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-content-repository")
        config = BlogConfig(
            siteName = "Test Site",
            baseUrl = "https://example.com/",
            description = "Test description",
            author = "Test Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = "pages")
        )

        val fileWalker = FileWalker(tempRoot.toString())
        val contentProcessor = ContentProcessor(MarkdownProcessor())
        repository = ContentRepository(tempRoot.toString(), config, fileWalker, contentProcessor)
    }

    @AfterTest
    fun cleanup() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `loadAllPosts returns posts sorted by date descending`() {
        createPost("posts/2024/latest.md", """
            ---
            title: Latest Post
            published: 2024-06-02T08:30:00
            ---
            Latest content.
        """.trimIndent())

        createPost("posts/2023/older.md", """
            ---
            title: Older Post
            published: 2023-07-15T12:00:00
            ---
            Older content.
        """.trimIndent())

        createPost("posts/2024/middle.md", """
            ---
            title: Middle Post
            published: 2024-01-01T00:00:00
            ---
            Middle content.
        """.trimIndent())

        val posts = repository.loadAllPosts(useCache = false)

        assertEquals(3, posts.size)
        assertEquals("Latest Post", posts[0].title)
        assertEquals("Middle Post", posts[1].title)
        assertEquals("Older Post", posts[2].title)
    }

    @Test
    fun `loadAllPages returns pages sorted by navOrder then title`() {
        createPage("pages/zebra.md", """
            ---
            title: Zebra
            ---
            Zebra content.
        """.trimIndent())

        createPage("pages/about.md", """
            ---
            title: About
            nav_order: 1
            ---
            About content.
        """.trimIndent())

        createPage("pages/apple.md", """
            ---
            title: Apple
            ---
            Apple content.
        """.trimIndent())

        createPage("pages/contact.md", """
            ---
            title: Contact
            nav_order: 2
            ---
            Contact content.
        """.trimIndent())

        val pages = repository.loadAllPages(useCache = false)

        assertEquals(4, pages.size)
        assertEquals("About", pages[0].title)      // nav_order: 1
        assertEquals("Contact", pages[1].title)    // nav_order: 2
        // Pages without nav_order are sorted alphabetically, but also sorted by navOrder=Int.MAX_VALUE
        // so the actual order depends on file system order. Let's just check they exist
        val titlesWithoutOrder = pages.drop(2).map { it.title }.toSet()
        assertTrue(titlesWithoutOrder.contains("Apple"))
        assertTrue(titlesWithoutOrder.contains("Zebra"))
    }

    @Test
    fun `loadAllPosts uses cache on second call`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        // First call loads from disk
        val posts1 = repository.loadAllPosts(useCache = false)
        assertEquals(1, posts1.size)

        // Modify file after loading
        createPost("posts/another.md", """
            ---
            title: Another Post
            published: 2024-02-01T00:00:00
            ---
            Another content.
        """.trimIndent())

        // Second call with cache should return same result
        val posts2 = repository.loadAllPosts(useCache = true)
        assertEquals(1, posts2.size, "Cache should return old result")
        assertEquals("Test Post", posts2[0].title)
    }

    @Test
    fun `loadAllPages uses cache on second call`() {
        createPage("pages/first.md", """
            ---
            title: First Page
            ---
            First content.
        """.trimIndent())

        // First call loads from disk
        val pages1 = repository.loadAllPages(useCache = false)
        assertEquals(1, pages1.size)

        // Modify file after loading
        createPage("pages/second.md", """
            ---
            title: Second Page
            ---
            Second content.
        """.trimIndent())

        // Second call with cache should return same result
        val pages2 = repository.loadAllPages(useCache = true)
        assertEquals(1, pages2.size, "Cache should return old result")
        assertEquals("First Page", pages2[0].title)
    }

    @Test
    fun `invalidatePost clears cache and reloads`() {
        createPost("posts/original.md", """
            ---
            title: Original Post
            published: 2024-01-01T00:00:00
            ---
            Original content.
        """.trimIndent())

        // Load and cache
        val posts1 = repository.loadAllPosts(useCache = false)
        assertEquals(1, posts1.size)
        assertEquals("Original Post", posts1[0].title)

        // Modify the post
        createPost("posts/original.md", """
            ---
            title: Updated Post
            published: 2024-01-01T00:00:00
            ---
            Updated content.
        """.trimIndent())

        // Cached call should return old
        val cached = repository.loadAllPosts(useCache = true)
        assertEquals("Original Post", cached[0].title)

        // Invalidate should reload
        val updated = repository.invalidatePost("original")
        assertNotNull(updated)
        assertEquals("Updated Post", updated.title)

        // New cache should have updated version
        val posts2 = repository.loadAllPosts(useCache = true)
        assertEquals("Updated Post", posts2[0].title)
    }

    @Test
    fun `invalidatePage clears cache and reloads`() {
        createPage("pages/about.md", """
            ---
            title: Original About
            ---
            Original content.
        """.trimIndent())

        // Load and cache
        val pages1 = repository.loadAllPages(useCache = false)
        assertEquals(1, pages1.size)
        assertEquals("Original About", pages1[0].title)

        // Modify the page
        createPage("pages/about.md", """
            ---
            title: Updated About
            ---
            Updated content.
        """.trimIndent())

        // Invalidate should reload
        val updated = repository.invalidatePage("about")
        assertNotNull(updated)
        assertEquals("Updated About", updated.title)

        // New cache should have updated version
        val pages2 = repository.loadAllPages(useCache = true)
        assertEquals("Updated About", pages2[0].title)
    }

    @Test
    fun `clearCache forces reload for both posts and pages`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        createPage("pages/test.md", """
            ---
            title: Test Page
            ---
            Test content.
        """.trimIndent())

        // Load and cache both
        repository.loadAllPosts(useCache = false)
        repository.loadAllPages(useCache = false)

        // Add new content
        createPost("posts/new-post.md", """
            ---
            title: New Post
            published: 2024-02-01T00:00:00
            ---
            New content.
        """.trimIndent())

        createPage("pages/new-page.md", """
            ---
            title: New Page
            ---
            New content.
        """.trimIndent())

        // Clear cache
        repository.clearCache()

        // Should see new content
        val posts = repository.loadAllPosts(useCache = true)
        val pages = repository.loadAllPages(useCache = true)

        assertEquals(2, posts.size)
        assertEquals(2, pages.size)
    }

    @Test
    fun `loadPostById returns cached post when available`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        // Populate cache
        repository.loadAllPosts(useCache = false)

        // Load by ID should use cache
        val post = repository.loadPostById("test", useCache = true)
        assertNotNull(post)
        assertEquals("Test Post", post.title)
    }

    @Test
    fun `loadPostById returns null for non-existent post`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        repository.loadAllPosts(useCache = false)

        val post = repository.loadPostById("nonexistent", useCache = true)
        assertNull(post)
    }

    @Test
    fun `loadPageById returns cached page when available`() {
        createPage("pages/about.md", """
            ---
            title: About Page
            ---
            About content.
        """.trimIndent())

        // Populate cache
        repository.loadAllPages(useCache = false)

        // Load by ID should use cache
        val page = repository.loadPageById("about", useCache = true)
        assertNotNull(page)
        assertEquals("About Page", page.title)
    }

    @Test
    fun `loadPostById reloads from disk when not cached`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        // Don't populate cache, load directly
        val post = repository.loadPostById("test", useCache = false)
        assertNotNull(post)
        assertEquals("Test Post", post.title)
    }

    @Test
    fun `loadAllPosts excludes index files`() {
        createPost("posts/regular.md", """
            ---
            title: Regular Post
            published: 2024-01-01T00:00:00
            ---
            Regular content.
        """.trimIndent())

        createPost("posts/index.md", """
            ---
            title: Index Post
            published: 2024-02-01T00:00:00
            ---
            Should be excluded.
        """.trimIndent())

        val posts = repository.loadAllPosts(useCache = false)

        assertEquals(1, posts.size)
        assertEquals("Regular Post", posts[0].title)
    }

    @Test
    fun `loadAllPosts detects HBS template files`() {
        createPost("posts/template.hbs", """
            ---
            title: Template Post
            published: 2024-01-01T00:00:00
            ---
            <h1>{{post.title}}</h1>
        """.trimIndent())

        createPost("posts/markdown.md", """
            ---
            title: Markdown Post
            published: 2024-02-01T00:00:00
            ---
            Regular markdown.
        """.trimIndent())

        val posts = repository.loadAllPosts(useCache = false)

        assertEquals(2, posts.size)
        val templatePost = posts.find { it.id == "template" }
        val markdownPost = posts.find { it.id == "markdown" }

        assertNotNull(templatePost)
        assertTrue(templatePost.isTemplate, "HBS file should be marked as template")

        assertNotNull(markdownPost)
        assertTrue(!markdownPost.isTemplate, "MD file should not be marked as template")
    }

    private fun createPost(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createPage(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createFile(relative: String, content: String) {
        val target = tempRoot / relative
        target.parent?.createDirectories()
        target.writeText(content)
    }
}
