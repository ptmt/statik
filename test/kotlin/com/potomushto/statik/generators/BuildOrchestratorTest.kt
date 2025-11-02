package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.processors.ContentProcessor
import com.potomushto.statik.processors.MarkdownProcessor
import com.potomushto.statik.template.HandlebarsTemplateEngine
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class BuildOrchestratorTest {

    private lateinit var tempRoot: Path
    private lateinit var config: BlogConfig
    private lateinit var orchestrator: BuildOrchestrator

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-build-orchestrator")
        config = BlogConfig(
            siteName = "Test Site",
            baseUrl = "https://example.com/",
            description = "Test description",
            author = "Test Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static", "public"), output = "build"),
            paths = PathConfig(posts = "posts", pages = "pages")
        )

        // Set up components
        val fileWalker = FileWalker(tempRoot.toString())
        val markdownProcessor = MarkdownProcessor()
        val contentProcessor = ContentProcessor(markdownProcessor)
        val templatesPath = tempRoot / config.theme.templates
        val templateEngine = HandlebarsTemplateEngine(templatesPath)

        val contentRepository = ContentRepository(
            tempRoot.toString(),
            config,
            fileWalker,
            contentProcessor
        )

        val templateRenderer = TemplateRenderer(
            templatesPath,
            templateEngine,
            config,
            null
        )

        val assetManager = AssetManager(
            tempRoot.toString(),
            config,
            fileWalker
        )

        val datasourceGenerator = StaticDatasourceGenerator(
            tempRoot,
            tempRoot / config.theme.output,
            config.staticDatasource,
            contentProcessor
        )

        val rssGenerator = RssGenerator(
            config,
            tempRoot / config.theme.output,
            null
        )

        orchestrator = BuildOrchestrator(
            tempRoot.toString(),
            config,
            contentRepository,
            templateRenderer,
            assetManager,
            rssGenerator,
            datasourceGenerator
        )

        // Create templates
        createTemplate("templates/layouts/default.hbs", """
            <html>
              <head><title>{{title}}</title></head>
              <body>{{{content}}}</body>
            </html>
        """.trimIndent())

        createTemplate("templates/home.hbs", """
            <h1>{{siteName}}</h1>
            {{#each posts}}<div class="post">{{title}}</div>{{/each}}
        """.trimIndent())

        createTemplate("templates/post.hbs", """
            <article><h1>{{post.title}}</h1>{{{post.content}}}</article>
        """.trimIndent())

        createTemplate("templates/page.hbs", """
            <div><h1>{{page.title}}</h1>{{{page.content}}}</div>
        """.trimIndent())
    }

    @AfterTest
    fun cleanup() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `buildFull generates all pages posts home RSS and assets`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        createPage("pages/about.md", """
            ---
            title: About
            ---
            About content.
        """.trimIndent())

        createAsset("static/style.css", "body { margin: 0; }")
        createAsset("public/robots.txt", "User-agent: *\nDisallow:")

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"

        // Check home page
        assertTrue((buildDir / "index.html").exists(), "Home page should exist")
        val homeHtml = (buildDir / "index.html").readText()
        assertTrue(homeHtml.contains("Test Site"))
        assertTrue(homeHtml.contains("Test Post"))

        // Check post
        assertTrue((buildDir / "test" / "index.html").exists(), "Post should exist")
        val postHtml = (buildDir / "test" / "index.html").readText()
        assertTrue(postHtml.contains("Test Post"))
        assertTrue(postHtml.contains("Test content"))

        // Check page
        assertTrue((buildDir / "about" / "index.html").exists(), "Page should exist")
        val pageHtml = (buildDir / "about" / "index.html").readText()
        assertTrue(pageHtml.contains("About"))

        // Check asset
        val staticCss = buildDir / "style.css"
        assertTrue(staticCss.exists(), "Static assets should be copied to build root")
        assertEquals("body { margin: 0; }", staticCss.readText())

        val robots = buildDir / "robots.txt"
        assertTrue(robots.exists(), "Public assets should be flattened to the build root")
        assertEquals("User-agent: *\nDisallow:", robots.readText())

        // Check RSS (default filename is feed.xml)
        assertTrue((buildDir / "feed.xml").exists(), "RSS feed should exist")
    }

    @Test
    fun `buildIncremental with post change rebuilds only that post plus home and RSS`() {
        // Initial build
        createPost("posts/first.md", """
            ---
            title: First Post
            published: 2024-01-01T00:00:00
            ---
            First content.
        """.trimIndent())

        createPost("posts/second.md", """
            ---
            title: Second Post
            published: 2024-02-01T00:00:00
            ---
            Second content.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"
        val firstPostPath = buildDir / "first" / "index.html"
        val secondPostPath = buildDir / "second" / "index.html"

        // Get initial timestamps
        val firstInitialTime = (firstPostPath).readText()
        val secondInitialTime = (secondPostPath).readText()

        // Modify first post
        createPost("posts/first.md", """
            ---
            title: First Post Updated
            published: 2024-01-01T00:00:00
            ---
            Updated content.
        """.trimIndent())

        // Incremental build with only first post changed
        val changedFiles = listOf(tempRoot / "posts" / "first.md")
        orchestrator.buildIncremental(changedFiles)

        // First post should be updated
        val firstUpdated = firstPostPath.readText()
        assertTrue(firstUpdated.contains("First Post Updated"))
        assertTrue(firstUpdated.contains("Updated content"))

        // Second post should remain unchanged (we can't verify timestamp without modifying file system)
        val secondAfter = secondPostPath.readText()
        assertTrue(secondAfter.contains("Second Post"))
        assertTrue(secondAfter.contains("Second content"))

        // Home page should be rebuilt (contains post list)
        val homeHtml = (buildDir / "index.html").readText()
        assertTrue(homeHtml.contains("First Post Updated"))
    }

    @Test
    fun `buildIncremental with page change rebuilds only that page`() {
        // Initial build
        createPage("pages/about.md", """
            ---
            title: About
            ---
            About content.
        """.trimIndent())

        createPage("pages/contact.md", """
            ---
            title: Contact
            ---
            Contact content.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"

        // Modify about page
        createPage("pages/about.md", """
            ---
            title: About Updated
            ---
            Updated about content.
        """.trimIndent())

        // Incremental build
        val changedFiles = listOf(tempRoot / "pages" / "about.md")
        orchestrator.buildIncremental(changedFiles)

        // About page should be updated
        val aboutHtml = (buildDir / "about" / "index.html").readText()
        assertTrue(aboutHtml.contains("About Updated"))
        assertTrue(aboutHtml.contains("Updated about content"))

        // Contact page should still exist
        val contactHtml = (buildDir / "contact" / "index.html").readText()
        assertTrue(contactHtml.contains("Contact"))
    }

    @Test
    fun `buildIncremental with template change triggers full rebuild`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"
        val initialHtml = (buildDir / "test" / "index.html").readText()
        assertTrue(initialHtml.contains("<article>"))

        // Modify post template
        createTemplate("templates/post.hbs", """
            <section><h2>{{post.title}}</h2>{{{post.content}}}</section>
        """.trimIndent())

        // Incremental build with template change
        val changedFiles = listOf(tempRoot / "templates" / "post.hbs")
        orchestrator.buildIncremental(changedFiles)

        // Should rebuild with new template
        val updatedHtml = (buildDir / "test" / "index.html").readText()
        assertTrue(updatedHtml.contains("<section>"))
        assertTrue(updatedHtml.contains("<h2>"))
    }

    @Test
    fun `buildIncremental with asset change copies only that asset`() {
        createAsset("static/style.css", "body { margin: 0; }")
        createAsset("static/script.js", "console.log('hello');")

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"

        // Modify one asset
        createAsset("static/style.css", "body { margin: 10px; }")

        // Incremental build
        val changedFiles = listOf(tempRoot / "static" / "style.css")
        orchestrator.buildIncremental(changedFiles)

        // Updated asset should have new content
        assertEquals("body { margin: 10px; }", (buildDir / "style.css").readText())

        // Other asset should still exist
        assertTrue((buildDir / "script.js").exists())
    }

    @Test
    fun `buildIncremental with config change triggers full rebuild`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"
        assertTrue((buildDir / "test" / "index.html").exists())

        // Simulate config change
        val changedFiles = listOf(tempRoot / "config.json")
        orchestrator.buildIncremental(changedFiles)

        // Should still rebuild everything
        assertTrue((buildDir / "index.html").exists())
        assertTrue((buildDir / "test" / "index.html").exists())
    }

    @Test
    fun `buildIncremental with multiple posts rebuilds all changed posts`() {
        createPost("posts/first.md", """
            ---
            title: First Post
            published: 2024-01-01T00:00:00
            ---
            First content.
        """.trimIndent())

        createPost("posts/second.md", """
            ---
            title: Second Post
            published: 2024-02-01T00:00:00
            ---
            Second content.
        """.trimIndent())

        createPost("posts/third.md", """
            ---
            title: Third Post
            published: 2024-03-01T00:00:00
            ---
            Third content.
        """.trimIndent())

        orchestrator.buildFull()

        // Modify two posts
        createPost("posts/first.md", """
            ---
            title: First Updated
            published: 2024-01-01T00:00:00
            ---
            Updated first.
        """.trimIndent())

        createPost("posts/third.md", """
            ---
            title: Third Updated
            published: 2024-03-01T00:00:00
            ---
            Updated third.
        """.trimIndent())

        val changedFiles = listOf(
            tempRoot / "posts" / "first.md",
            tempRoot / "posts" / "third.md"
        )
        orchestrator.buildIncremental(changedFiles)

        val buildDir = tempRoot / "build"

        // Both changed posts should be updated
        val firstHtml = (buildDir / "first" / "index.html").readText()
        assertTrue(firstHtml.contains("First Updated"))

        val thirdHtml = (buildDir / "third" / "index.html").readText()
        assertTrue(thirdHtml.contains("Third Updated"))

        // Unchanged post should still exist
        val secondHtml = (buildDir / "second" / "index.html").readText()
        assertTrue(secondHtml.contains("Second Post"))
    }

    @Test
    fun `buildIncremental with empty file list does nothing`() {
        createPost("posts/test.md", """
            ---
            title: Test Post
            published: 2024-01-01T00:00:00
            ---
            Test content.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"
        assertTrue((buildDir / "test" / "index.html").exists())

        // Incremental build with no changes
        orchestrator.buildIncremental(emptyList())

        // Everything should still exist
        assertTrue((buildDir / "test" / "index.html").exists())
    }

    @Test
    fun `buildSinglePost creates post at correct path`() {
        createPost("posts/2024/my-post.md", """
            ---
            title: My Post
            published: 2024-01-01T00:00:00
            ---
            Post content.
        """.trimIndent())

        // Load content to populate context
        orchestrator.buildFull()

        val buildDir = tempRoot / "build"

        // Post should be at /2024/my-post/index.html
        assertTrue((buildDir / "2024" / "my-post" / "index.html").exists())
        val postHtml = (buildDir / "2024" / "my-post" / "index.html").readText()
        assertTrue(postHtml.contains("My Post"))
        assertTrue(postHtml.contains("Post content"))
    }

    @Test
    fun `buildSinglePage creates page at correct path`() {
        createPage("pages/docs/guide.md", """
            ---
            title: Guide
            ---
            Guide content.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"

        // Page should be at /docs/guide/index.html
        assertTrue((buildDir / "docs" / "guide" / "index.html").exists())
        val pageHtml = (buildDir / "docs" / "guide" / "index.html").readText()
        assertTrue(pageHtml.contains("Guide"))
    }

    @Test
    fun `buildHomePage includes posts in correct order`() {
        createPost("posts/newest.md", """
            ---
            title: Newest Post
            published: 2024-03-01T00:00:00
            ---
            Newest.
        """.trimIndent())

        createPost("posts/oldest.md", """
            ---
            title: Oldest Post
            published: 2024-01-01T00:00:00
            ---
            Oldest.
        """.trimIndent())

        createPost("posts/middle.md", """
            ---
            title: Middle Post
            published: 2024-02-01T00:00:00
            ---
            Middle.
        """.trimIndent())

        orchestrator.buildFull()

        val buildDir = tempRoot / "build"
        val homeHtml = (buildDir / "index.html").readText()

        // Posts should be in reverse chronological order
        val newestIndex = homeHtml.indexOf("Newest Post")
        val middleIndex = homeHtml.indexOf("Middle Post")
        val oldestIndex = homeHtml.indexOf("Oldest Post")

        assertTrue(newestIndex < middleIndex, "Newest should come before middle")
        assertTrue(middleIndex < oldestIndex, "Middle should come before oldest")
    }

    @Test
    fun `buildIncremental handles mixed changes correctly`() {
        createPost("posts/blog.md", """
            ---
            title: Blog Post
            published: 2024-01-01T00:00:00
            ---
            Blog content.
        """.trimIndent())

        createPage("pages/about.md", """
            ---
            title: About
            ---
            About content.
        """.trimIndent())

        createAsset("static/style.css", "body { margin: 0; }")

        orchestrator.buildFull()

        // Modify all types
        createPost("posts/blog.md", """
            ---
            title: Blog Updated
            published: 2024-01-01T00:00:00
            ---
            Updated blog.
        """.trimIndent())

        createPage("pages/about.md", """
            ---
            title: About Updated
            ---
            Updated about.
        """.trimIndent())

        createAsset("static/style.css", "body { margin: 10px; }")

        val changedFiles = listOf(
            tempRoot / "posts" / "blog.md",
            tempRoot / "pages" / "about.md",
            tempRoot / "static" / "style.css"
        )
        orchestrator.buildIncremental(changedFiles)

        val buildDir = tempRoot / "build"

        // All should be updated
        val blogHtml = (buildDir / "blog" / "index.html").readText()
        assertTrue(blogHtml.contains("Blog Updated"))

        val aboutHtml = (buildDir / "about" / "index.html").readText()
        assertTrue(aboutHtml.contains("About Updated"))

        val css = (buildDir / "style.css").readText()
        assertEquals("body { margin: 10px; }", css)
    }

    private fun createPost(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createPage(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createAsset(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createTemplate(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createFile(relative: String, content: String) {
        val target = tempRoot / relative
        target.parent?.createDirectories()
        target.writeText(content)
    }
}
