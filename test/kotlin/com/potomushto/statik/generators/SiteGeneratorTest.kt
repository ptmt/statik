package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import java.nio.file.Files
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
class SiteGeneratorTest {

    private lateinit var tempRoot: Path

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-site-generator")
    }

    @AfterTest
    fun cleanup() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `generate creates home page posts pages and static assets`() {
        val config = BlogConfig(
            siteName = "Test Site",
            baseUrl = "https://example.com/",
            description = "Sample description",
            author = "Author",
            theme = ThemeConfig(templates = "templates", assets = "static", output = "build"),
            paths = PathConfig(posts = "posts", pages = "pages")
        )

        createPost("posts/2024/latest.md", """
            ---
            title: Latest Post
            published: 2024-06-02T08:30:00
            ---
            # Latest content
            Most recent entry.
        """.trimIndent())

        createPost("posts/2023/older.md", """
            ---
            title: Older Post
            published: 2023-07-15T12:00:00
            ---
            # Older content
            First entry.
        """.trimIndent())

        createPage("pages/about.md", """
            ---
            title: About
            nav_order: 1
            ---
            ## About
            About page content.
        """.trimIndent())

        createPage("pages/docs/index.md", """
            ---
            title: Docs
            nav_order: 2
            ---
            ## Docs
            Documentation landing page.
        """.trimIndent())

        createStaticAsset("static/css/site.css", "body { color: #222; }")

        writeTemplate("templates/home.hbs", """
            <header>{{siteName}}</header>
            <section class="posts">
            {{#each posts}}
              <article class="post">{{title}}</article>
            {{/each}}
            </section>
            <nav class="pages">
            {{#each pages}}
              <span class="page">{{title}}</span>
            {{/each}}
            </nav>
        """.trimIndent())

        writeTemplate("templates/post.hbs", """
            <h1>{{post.title}}</h1>
            <div class="post-body">{{{post.content}}}</div>
        """.trimIndent())

        writeTemplate("templates/page.hbs", """
            <h1>{{page.title}}</h1>
            <div class="page-body">{{{page.content}}}</div>
        """.trimIndent())

        writeTemplate("templates/layouts/default.hbs", """
            <html>
              <body>
                <header>{{siteName}}</header>
                <main>{{{content}}}</main>
                <footer>{{description}}</footer>
                <nav>
                  {{#each pages}}<span class="nav-item">{{title}}</span>{{/each}}
                </nav>
              </body>
            </html>
        """.trimIndent())

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val buildRoot = tempRoot / "build"
        val homeHtml = (buildRoot / "index.html").readText()

        assertTrue((buildRoot / "index.html").exists())
        assertTrue(homeHtml.contains("Test Site"))
        assertTrue(homeHtml.indexOf("Latest Post") < homeHtml.indexOf("Older Post"))
        assertTrue(homeHtml.indexOf("About") < homeHtml.indexOf("Docs"))

        val postHtml = (buildRoot / "2024" / "latest" / "index.html").readText()
        assertTrue(postHtml.contains("<header>Test Site</header>"))
        assertTrue(postHtml.contains("Most recent entry."))

        val olderPost = (buildRoot / "2023" / "older" / "index.html").readText()
        assertTrue(olderPost.contains("First entry."))

        val aboutHtml = (buildRoot / "about" / "index.html").readText()
        assertTrue(aboutHtml.contains("About page content."))

        val docsHtml = (buildRoot / "docs" / "index.html").readText()
        assertTrue(docsHtml.contains("Documentation landing page."))

        val assetPath = buildRoot / "css" / "site.css"
        assertTrue(assetPath.exists())
        assertEquals("body { color: #222; }", assetPath.readText())
    }

    @Test
    fun `generate falls back to built in templates when custom ones missing`() {
        val config = BlogConfig(
            siteName = "Fallback Site",
            baseUrl = "https://fallback.example/",
            description = "Fallback description",
            author = "Author",
            theme = ThemeConfig(templates = "missing-templates", assets = "assets", output = "build"),
            paths = PathConfig(posts = "posts", pages = "pages")
        )

        createPost("posts/post.md", """
            ---
            title: Fallback Post
            published: 2024-04-01T00:00:00
            ---
            Content for fallback.
        """.trimIndent())

        createPage("pages/about.md", """
            ---
            title: About Fallback
            ---
            Content for fallback page.
        """.trimIndent())

        createStaticAsset("assets/logo.txt", "logo")

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val homeHtml = (tempRoot / "build" / "index.html").readText()
        assertTrue(homeHtml.contains("Fallback Site"))
        assertTrue(homeHtml.contains("class=\"post-list\""))

        val postHtml = (tempRoot / "build" / "post" / "index.html").readText()
        assertTrue(postHtml.contains("Fallback Post"))

        val pageHtml = (tempRoot / "build" / "about" / "index.html").readText()
        assertTrue(pageHtml.contains("About Fallback"))

        val assetPath = tempRoot / "build" / "logo.txt"
        assertTrue(assetPath.exists())
        assertEquals("logo", assetPath.readText())
    }

    private fun createPost(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createPage(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createStaticAsset(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun writeTemplate(relative: String, content: String) {
        createFile(relative, content)
    }

    private fun createFile(relative: String, content: String) {
        val target = resolve(relative)
        target.parent?.createDirectories()
        target.writeText(content)
    }

    private fun resolve(relative: String): Path = tempRoot / relative
}
