package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.generators.CollectableDatasourceItem
import com.potomushto.statik.generators.EntityDatasourceItem
import com.potomushto.statik.generators.ImageDatasourceItem
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
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
        val latestIndex = homeHtml.indexOf("Latest Post")
        val olderIndex = homeHtml.indexOf("Older Post")
        assertTrue(latestIndex >= 0, "Home page should list Latest Post")
        assertTrue(olderIndex >= 0, "Home page should list Older Post")
        assertTrue(latestIndex < olderIndex, "Latest Post should appear before Older Post")
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
            theme = ThemeConfig(templates = "missing-templates", assets = listOf("assets"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
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

        val assetPath = tempRoot / "build" / "assets" / "logo.txt"
        assertTrue(assetPath.exists())
        assertEquals("logo", assetPath.readText())
    }

    @Test
    fun `generate processes HTML files in posts and pages`() {
        val config = BlogConfig(
            siteName = "HTML Test Site",
            baseUrl = "https://html.example/",
            description = "Test HTML files",
            author = "Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
        )

        // Create an HTML post with frontmatter
        createPost("posts/html-post.html", """
            ---
            title: HTML Post
            published: 2024-07-01T10:00:00
            description: A post written in HTML
            ---
            <h1>HTML Content</h1>
            <p>This is an HTML post with custom markup.</p>
        """.trimIndent())

        // Create an HTML page with frontmatter
        createPage("pages/html-page.html", """
            ---
            title: HTML Page
            nav_order: 1
            ---
            <h2>Custom HTML Page</h2>
            <div class="custom">HTML content directly</div>
        """.trimIndent())

        writeTemplate("templates/post.hbs", "<article>{{post.title}}: {{{post.content}}}</article>")
        writeTemplate("templates/page.hbs", "<section>{{page.title}}: {{{page.content}}}</section>")

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val postHtml = (tempRoot / "build" / "html-post" / "index.html").readText()
        assertTrue(postHtml.contains("HTML Post"))
        assertTrue(postHtml.contains("<h1>HTML Content</h1>"))
        assertTrue(postHtml.contains("This is an HTML post with custom markup."))

        val pageHtml = (tempRoot / "build" / "html-page" / "index.html").readText()
        assertTrue(pageHtml.contains("HTML Page"))
        assertTrue(pageHtml.contains("<h2>Custom HTML Page</h2>"))
        assertTrue(pageHtml.contains("class=\"custom\""))
    }

    @Test
    fun `generate writes static datasource files`() {
        val config = BlogConfig(
            siteName = "Datasource Site",
            baseUrl = "https://datasource.example/",
            description = "Datasource demo",
            author = "Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
        )

        createPost("posts/gallery.md", """
            ---
            title: Gallery
            published: 2024-01-01T00:00:00
            ---
            ![Sunrise](/media/sunrise.jpg)

            <div data-collect="quotes" data-author="Alice">The dawn arrives.</div>
        """.trimIndent())

        createPage("pages/about.html", """
            ---
            title: About
            ---
            <img src="/media/about.jpg" alt="About" title="About us" />
            <blockquote data-collect="quotes" data-author="Bob">We build things.</blockquote>
        """.trimIndent())

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val datasourceDir = tempRoot / "build" / "datasource"
        val json = Json { ignoreUnknownKeys = false }

        val imagesPath = datasourceDir / "images.json"
        assertTrue(imagesPath.exists())
        val images = json.decodeFromString<List<ImageDatasourceItem>>(imagesPath.readText())
        assertEquals(2, images.size)
        val sunrise = images.first { it.src == "/media/sunrise.jpg" }
        assertEquals("Sunrise", sunrise.alt)
        assertEquals("post", sunrise.source.type)
        assertEquals("/gallery/", sunrise.source.path)

        val aboutImage = images.first { it.src == "/media/about.jpg" }
        assertEquals("About", aboutImage.alt)
        assertEquals("About us", aboutImage.title)
        assertEquals("page", aboutImage.source.type)

        val quotesPath = datasourceDir / "quotes.json"
        assertTrue(quotesPath.exists())
        val quotes = json.decodeFromString<List<CollectableDatasourceItem>>(quotesPath.readText())
        assertEquals(2, quotes.size)
        val aliceQuote = quotes.first { it.source.id == "gallery" }
        assertEquals("The dawn arrives.", aliceQuote.text)
        assertEquals("Alice", aliceQuote.attributes["data-author"])
        val bobQuote = quotes.first { it.source.type == "page" }
        assertEquals("We build things.", bobQuote.text)
        assertEquals("Bob", bobQuote.attributes["data-author"])
    }

    @Test
    fun `generate exposes datasource to templates`() {
        val config = BlogConfig(
            siteName = "Datasource Template Site",
            baseUrl = "https://datasource-template.example/",
            description = "Datasource template demo",
            author = "Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
        )

        (tempRoot / "datasource-config.json").writeText(
            """
                {
                  "datasets": [
                    {
                      "name": "team",
                      "output": "entity-datasource.json",
                      "folder": "entities",
                      "metadataKey": "collectAs",
                      "metadataValue": "team"
                    }
                  ]
                }
            """.trimIndent()
        )

        createFile("entities/alice.md", """
            ---
            id: alice
            title: Alice Doe
            role: Engineer
            ---
            Alice keeps the CI green.
        """.trimIndent())

        createPost("posts/story.md", """
            ---
            title: Story
            published: 2024-03-01T00:00:00
            collectAs: team
            ---
            ![Hero](/media/hero.jpg)

            <blockquote data-collect="quotes">First quote</blockquote>
        """.trimIndent())

        writeTemplate("templates/home.hbs", """
            {{#with datasource}}
              <div class="entity">{{entities.team.[0].title}}</div>
              <div class="quote">{{collectables.quotes.[0].text}}</div>
              <div class="image">{{images.[0].src}}</div>
              <div class="dataset">{{datasets.[0].output}}</div>
            {{/with}}
        """.trimIndent())

        writeTemplate("templates/layouts/default.hbs", """
            <html>
              <body>{{{content}}}</body>
            </html>
        """.trimIndent())

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val homeHtml = (tempRoot / "build" / "index.html").readText()
        assertTrue(homeHtml.contains("Alice Doe"))
        assertTrue(homeHtml.contains("First quote"))
        assertTrue(homeHtml.contains("/media/hero.jpg"))
        assertTrue(homeHtml.contains("entity-datasource.json"))
    }

    @Test
    fun `generate writes configured entity datasets`() {
        val config = BlogConfig(
            siteName = "Entity Site",
            baseUrl = "https://entity.example/",
            description = "Entity demo",
            author = "Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
        )

        (tempRoot / "datasource-config.json").writeText(
            """
                {
                  "datasets": [
                    {
                      "name": "team",
                      "output": "entity-datasource.json",
                      "folder": "entities",
                      "metadataKey": "collectAs",
                      "metadataValue": "team",
                      "includeSources": ["posts", "pages"]
                    }
                  ]
                }
            """.trimIndent()
        )

        createFile("entities/alice.md", """
            ---
            id: alice
            title: Alice
            role: Engineer
            ---
            Alice builds things.
        """.trimIndent())

        createFile("entities/bob.html", """
            ---
            id: bob
            title: Bob
            role: Designer
            ---
            <p>Bob designs experiences.</p>
        """.trimIndent())

        createPost("posts/profile.md", """
            ---
            title: Profile
            published: 2024-02-01T00:00:00
            collectAs: team
            ---
            Team profile entry.
        """.trimIndent())

        createPage("pages/overview.md", """
            ---
            title: Overview
            collectAs: team
            ---
            Overview of the team.
        """.trimIndent())

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val datasourceDir = tempRoot / "build" / "datasource"
        val json = Json { ignoreUnknownKeys = false }

        val entitiesPath = datasourceDir / "entity-datasource.json"
        assertTrue(entitiesPath.exists())
        val entities = json.decodeFromString<List<EntityDatasourceItem>>(entitiesPath.readText())
        assertEquals(4, entities.size)
        assertTrue(entities.all { it.dataset == "team" })

        val alice = entities.first { it.id == "alice" }
        assertEquals("Engineer", alice.metadata["role"])
        assertEquals("team", alice.source.type)
        assertEquals("/entities/alice/", alice.source.path)

        val profile = entities.first { it.source.type == "post" }
        assertEquals("Profile", profile.title)
        assertEquals("team", profile.metadata["collectAs"])

        val overview = entities.first { it.source.type == "page" }
        assertEquals("Overview", overview.title)
        assertEquals("/overview/", overview.source.path)
    }

    @Test
    fun `generate processes HBS template files in posts and pages`() {
        val config = BlogConfig(
            siteName = "HBS Test Site",
            baseUrl = "https://hbs.example/",
            description = "Test HBS files",
            author = "Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages"))
        )

        // Create an HBS post with frontmatter and template variables
        createPost("posts/template-post.hbs", """
            ---
            title: Template Post
            published: 2024-08-01T10:00:00
            custom_var: Hello World
            ---
            <h1>{{post.title}}</h1>
            <p>Site: {{siteName}}</p>
            <p>Custom: {{post.metadata.custom_var}}</p>
        """.trimIndent())

        // Create an HBS page that uses template variables
        createPage("pages/template-page.hbs", """
            ---
            title: Template Page
            nav_order: 1
            greeting: Welcome
            ---
            <h2>{{page.title}}</h2>
            <p>{{page.metadata.greeting}} to {{siteName}}</p>
            <ul>
            {{#each pages}}
              <li>{{title}}</li>
            {{/each}}
            </ul>
        """.trimIndent())

        writeTemplate("templates/layouts/default.hbs", """
            <html>
            <head><title>{{title}}</title></head>
            <body>{{{content}}}</body>
            </html>
        """.trimIndent())

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        val postHtml = (tempRoot / "build" / "template-post" / "index.html").readText()
        assertTrue(postHtml.contains("<h1>Template Post</h1>"))
        assertTrue(postHtml.contains("<p>Site: HBS Test Site</p>"))
        assertTrue(postHtml.contains("<p>Custom: Hello World</p>"))

        val pageHtml = (tempRoot / "build" / "template-page" / "index.html").readText()
        assertTrue(pageHtml.contains("<h2>Template Page</h2>"))
        assertTrue(pageHtml.contains("Welcome to HBS Test Site"))
        assertTrue(pageHtml.contains("<li>Template Page</li>"))
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
