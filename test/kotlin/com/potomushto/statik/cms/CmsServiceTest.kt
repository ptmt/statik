package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.CmsConfig
import com.potomushto.statik.config.CmsGitConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.generators.SiteGenerator
import org.eclipse.jgit.api.Git
import java.util.Base64
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
    fun `save with previous source path renames content and cleans previous output`() {
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
                sourcePath = "posts/renamed.md",
                previousSourcePath = "posts/hello.md",
                frontmatter = """
                    title: After
                    published: 2024-01-02T10:45:00
                """.trimIndent(),
                body = "Renamed content."
            )
        )

        assertEquals("posts/renamed.md", response.item.sourcePath)
        assertFalse((tempRoot / "posts" / "hello.md").exists())
        assertTrue((tempRoot / "posts" / "renamed.md").exists())
        assertFalse((tempRoot / "build" / "hello").exists())
        assertTrue((tempRoot / "build" / "renamed" / "index.html").exists())
        assertEquals(listOf("posts/renamed.md"), service.list().items.map { it.sourcePath })
        assertEquals(2, service.status().dirty)
    }

    @Test
    fun `list follows site ordering and marks drafts`() {
        createPost(
            "posts/older.md",
            """
                ---
                title: Older
                published: 2024-01-01T09:30:00
                ---
                Older body.
            """.trimIndent()
        )
        createPost(
            "posts/draft.md",
            """
                ---
                title: Draft
                published: 2024-01-02T09:30:00
                draft: true
                ---
                Draft body.
            """.trimIndent()
        )
        createPost(
            "posts/newer.md",
            """
                ---
                title: Newer
                published: 2024-01-03T09:30:00
                ---
                Newer body.
            """.trimIndent()
        )
        createPage(
            "pages/docs.md",
            """
                ---
                title: Docs
                nav_order: 2
                ---
                Docs page.
            """.trimIndent()
        )
        createPage(
            "pages/about.md",
            """
                ---
                title: About
                nav_order: 1
                ---
                About page.
            """.trimIndent()
        )

        val service = createCmsService()

        val pageItems = service.list(CmsContentType.PAGE).items
        assertEquals(listOf("pages/about.md", "pages/docs.md"), pageItems.map { it.sourcePath })
        assertEquals(listOf(1, 2), pageItems.map { it.navOrder })

        val postItems = service.list(CmsContentType.POST).items
        assertEquals(listOf("posts/newer.md", "posts/older.md", "posts/draft.md"), postItems.map { it.sourcePath })
        assertEquals(listOf(false, false, true), postItems.map { it.isDraft })
    }

    @Test
    fun `media list includes nested assets and public paths`() {
        (tempRoot / "static" / "media").createDirectories()
        (tempRoot / "static" / "docs").createDirectories()
        (tempRoot / "static" / "media" / "hero.png").writeText("hero")
        (tempRoot / "static" / "docs" / "readme.pdf").writeText("readme")

        val service = createCmsService()
        val media = service.listMedia()

        assertEquals(listOf("static"), media.roots)
        assertEquals(2, media.total)
        assertEquals(0, media.dirty)
        assertEquals(
            listOf("static/docs/readme.pdf", "static/media/hero.png"),
            media.items.map { it.sourcePath }
        )
        assertEquals(
            listOf("/docs/readme.pdf", "/media/hero.png"),
            media.items.map { it.publicPath }
        )
    }

    @Test
    fun `upload rename and delete media folders updates source and output`() {
        val service = createCmsService()

        service.uploadMedia(
            CmsMediaUploadRequest(
                targetDirectory = "static/gallery",
                fileName = "hero.png",
                contentsBase64 = Base64.getEncoder().encodeToString("hero".toByteArray())
            )
        )
        service.uploadMedia(
            CmsMediaUploadRequest(
                targetDirectory = "static/gallery/icons",
                fileName = "icon.svg",
                contentsBase64 = Base64.getEncoder().encodeToString("icon".toByteArray())
            )
        )

        assertTrue((tempRoot / "static" / "gallery" / "hero.png").exists())
        assertTrue((tempRoot / "static" / "gallery" / "icons" / "icon.svg").exists())
        assertTrue((tempRoot / "build" / "gallery" / "hero.png").exists())
        assertTrue((tempRoot / "build" / "gallery" / "icons" / "icon.svg").exists())

        val rename = service.renameMedia(
            CmsMediaRenameRequest(
                sourcePath = "static/gallery",
                targetPath = "static/portfolio"
            )
        )

        assertEquals("static/portfolio", rename.selectedPath)
        assertFalse((tempRoot / "static" / "gallery" / "hero.png").exists())
        assertFalse((tempRoot / "build" / "gallery" / "hero.png").exists())
        assertTrue((tempRoot / "static" / "portfolio" / "hero.png").exists())
        assertTrue((tempRoot / "static" / "portfolio" / "icons" / "icon.svg").exists())
        assertTrue((tempRoot / "build" / "portfolio" / "hero.png").exists())
        assertTrue((tempRoot / "build" / "portfolio" / "icons" / "icon.svg").exists())
        assertEquals(
            listOf("static/portfolio/hero.png", "static/portfolio/icons/icon.svg"),
            service.listMedia().items.map { it.sourcePath }
        )

        val deletion = service.deleteMedia(CmsMediaDeleteRequest("static/portfolio"))

        assertEquals(2, deletion.affectedPaths.size)
        assertFalse((tempRoot / "static" / "portfolio" / "hero.png").exists())
        assertFalse((tempRoot / "build" / "portfolio" / "hero.png").exists())
        assertEquals(0, service.listMedia().total)
        assertEquals(0, service.listMedia().dirty)
        assertEquals(0, service.status().dirty)
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

    @Test
    fun `sync after content rename stages new and deleted paths`() {
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
                sourcePath = "posts/renamed.md",
                previousSourcePath = "posts/hello.md",
                frontmatter = """
                    title: Renamed
                    published: 2024-01-03T11:00:00
                """.trimIndent(),
                body = "Renamed content."
            )
        )

        val sync = service.sync("cms: rename hello", push = false)

        assertTrue(sync.committed)
        assertEquals("cms: rename hello", latestCommitMessage())
        assertEquals(0, sync.dirtyRemaining)
        Git.open(tempRoot.toFile()).use { git ->
            val status = git.status().call()
            assertTrue(status.isClean)
        }
        assertFalse((tempRoot / "posts" / "hello.md").exists())
        assertTrue((tempRoot / "posts" / "renamed.md").exists())
    }

    @Test
    fun `sync commits dirty media changes and clears dirty state`() {
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

        service.uploadMedia(
            CmsMediaUploadRequest(
                targetDirectory = "static/gallery",
                fileName = "hero.png",
                contentsBase64 = Base64.getEncoder().encodeToString("hero".toByteArray())
            )
        )

        val sync = service.sync("cms: update media", push = false)

        assertTrue(sync.committed)
        assertEquals("cms: update media", latestCommitMessage())
        assertEquals(0, sync.dirtyRemaining)
        assertEquals(0, service.listMedia().dirty)
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
