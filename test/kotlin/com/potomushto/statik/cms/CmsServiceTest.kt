package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.CmsConfig
import com.potomushto.statik.config.CmsGitConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.generators.SiteGenerator
import org.eclipse.jgit.api.Git
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class CmsServiceTest {

    private lateinit var tempRoot: java.nio.file.Path
    private lateinit var config: BlogConfig

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-cms-service")
        (tempRoot / "posts").createDirectories()
        (tempRoot / "pages").createDirectories()
        (tempRoot / "static").createDirectories()

        config = BlogConfig(
            siteName = "CMS Test",
            baseUrl = "https://example.com",
            description = "CMS integration tests",
            author = "Test Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages")),
            cms = CmsConfig(
                enabled = true,
                git = CmsGitConfig(
                    enabled = true,
                    authorName = "Statik CMS",
                    authorEmail = "cms@example.com"
                )
            )
        )
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `bootstrap indexes content into sqlite`() {
        createPost(
            "posts/hello.md",
            """
                ---
                title: Hello CMS
                published: 2024-01-01T09:30:00
                ---
                # Hello
            """.trimIndent()
        )
        createPage(
            "pages/about.md",
            """
                ---
                title: About
                ---
                About page.
            """.trimIndent()
        )

        val service = createCmsService()
        val list = service.list()

        assertEquals(2, list.total)
        assertEquals(0, list.dirty)
        assertTrue((tempRoot / ".statik" / "cms.db").exists())

        val post = service.get("posts/hello.md")
        assertEquals("Hello CMS", post.title)
        assertEquals("2024-01-01T09:30:00", post.publishedAt)
        assertFalse(post.dirty)
    }

    @Test
    fun `save updates source file and rebuilds output`() {
        createPost(
            "posts/hello.md",
            """
                ---
                title: Before
                published: 2024-01-01T09:30:00
                ---
                Before body.
            """.trimIndent()
        )

        val service = createCmsService()

        val response = service.save(
            CmsSaveRequest(
                type = CmsContentType.POST,
                sourcePath = "posts/hello.md",
                frontmatter = """
                    title: After
                    published: 2024-01-02T10:45:00
                """.trimIndent(),
                body = """
                    # Updated

                    Fresh content.
                """.trimIndent()
            )
        )

        assertTrue(response.item.dirty)
        assertTrue((tempRoot / "posts" / "hello.md").readText().contains("title: After"))
        assertTrue((tempRoot / "posts" / "hello.md").readText().contains("Fresh content."))
        assertTrue((tempRoot / "build" / "hello" / "index.html").readText().contains("Fresh content."))
        assertEquals(1, service.list().dirty)
    }

    @Test
    fun `sync commits dirty CMS changes and clears dirty state`() {
        createPost(
            "posts/hello.md",
            """
                ---
                title: Before
                published: 2024-01-01T09:30:00
                ---
                Before body.
            """.trimIndent()
        )

        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()

        Git.init().setDirectory(tempRoot.toFile()).call().use { git ->
            git.add().addFilepattern(".").call()
            git.commit()
                .setMessage("init")
                .setAuthor("Init", "init@example.com")
                .setCommitter("Init", "init@example.com")
                .call()
        }

        val service = CmsService(tempRoot, config, generator).also { it.bootstrap() }

        service.save(
            CmsSaveRequest(
                type = CmsContentType.POST,
                sourcePath = "posts/hello.md",
                frontmatter = """
                    title: Synced
                    published: 2024-01-03T11:00:00
                """.trimIndent(),
                body = "Synced content."
            )
        )

        val sync = service.sync("cms: update hello", push = false)

        assertTrue(sync.committed)
        assertEquals("cms: update hello", latestCommitMessage())
        assertEquals(0, sync.dirtyRemaining)
        assertFalse(service.get("posts/hello.md").dirty)
        assertEquals(0, service.status().dirty)
    }

    private fun createCmsService(): CmsService {
        val generator = SiteGenerator(tempRoot.toString(), config)
        generator.generate()
        return CmsService(tempRoot, config, generator).also { it.bootstrap() }
    }

    private fun createPost(path: String, contents: String) {
        val file = tempRoot / path
        file.parent.createDirectories()
        file.writeText(contents)
    }

    private fun createPage(path: String, contents: String) {
        val file = tempRoot / path
        file.parent.createDirectories()
        file.writeText(contents)
    }

    private fun latestCommitMessage(): String {
        Git.open(tempRoot.toFile()).use { git ->
            return git.log().setMaxCount(1).call().first().fullMessage
        }
    }
}
