package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.CmsConfig
import com.potomushto.statik.config.CmsGitConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.generators.SiteGenerator
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class CmsServiceTest {

    private lateinit var tempRoot: java.nio.file.Path
    private lateinit var config: BlogConfig
    private val extraRoots = mutableListOf<java.nio.file.Path>()

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
        extraRoots.forEach { it.deleteRecursively() }
        extraRoots.clear()
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
    fun `preview serves draft content from cms preview root`() {
        createPost(
            "posts/draft.md",
            """
                ---
                title: Draft Preview
                published: 2024-01-02T09:30:00
                draft: true
                ---
                Draft body.
            """.trimIndent()
        )

        val service = createCmsService()

        assertFalse((tempRoot / "build" / "draft").exists())

        val previewPost = assertNotNull(service.resolvePreviewFile("draft"))
        val previewHome = assertNotNull(service.resolvePreviewFile(""))

        assertTrue(previewPost.readText().contains("Draft body."))
        assertTrue(previewHome.readText().contains("Draft Preview"))
        assertTrue(previewHome.readText().contains("/__statik__/cms/preview/draft/"))
    }

    @Test
    fun `cms shared stylesheets expose configured theme css`() {
        (tempRoot / "static" / "css").createDirectories()
        (tempRoot / "static" / "css" / "tokens.css").writeText(":root { --accent: #2f5d50; }")

        val configWithSharedStyles = config.copy(
            cms = config.cms.copy(sharedStylesheets = listOf("static/css/tokens.css"))
        )
        val generator = SiteGenerator(tempRoot.toString(), configWithSharedStyles)
        generator.generate()

        val service = CmsService(tempRoot, configWithSharedStyles, generator).also { it.bootstrap() }

        assertEquals(
            listOf("/__statik__/cms/theme-assets/static/css/tokens.css"),
            service.sharedStylesheetHrefs()
        )

        val stylesheet = assertNotNull(service.resolveSharedStylesheetFile("static/css/tokens.css"))
        assertEquals(":root { --accent: #2f5d50; }", stylesheet.readText())
    }

    @Test
    fun `preview stays current after saving when preview was already opened`() {
        createPost(
            "posts/draft.md",
            """
                ---
                title: Draft Preview
                published: 2024-01-02T09:30:00
                draft: true
                ---
                Before preview update.
            """.trimIndent()
        )

        val service = createCmsService()
        assertNotNull(service.resolvePreviewFile("draft"))

        service.save(
            CmsSaveRequest(
                type = CmsContentType.POST,
                sourcePath = "posts/draft.md",
                frontmatter = """
                    title: Draft Preview
                    published: 2024-01-02T09:30:00
                    draft: true
                """.trimIndent(),
                body = "After preview update."
            )
        )

        val updatedPreview = assertNotNull(service.resolvePreviewFile("draft"))
        assertTrue(updatedPreview.readText().contains("After preview update."))
        assertFalse(updatedPreview.readText().contains("Before preview update."))
    }

    @Test
    fun `saving a published post as draft removes its public output but keeps preview`() {
        createPost(
            "posts/draft.md",
            """
                ---
                title: Draft Preview
                published: 2024-01-02T09:30:00
                ---
                Public body.
            """.trimIndent()
        )

        val service = createCmsService()
        assertTrue((tempRoot / "build" / "draft" / "index.html").exists())

        service.save(
            CmsSaveRequest(
                type = CmsContentType.POST,
                sourcePath = "posts/draft.md",
                frontmatter = """
                    title: Draft Preview
                    published: 2024-01-02T09:30:00
                    draft: true
                """.trimIndent(),
                body = "Private draft body."
            )
        )

        assertFalse((tempRoot / "build" / "draft" / "index.html").exists())
        val preview = assertNotNull(service.resolvePreviewFile("draft"))
        assertTrue(preview.readText().contains("Private draft body."))
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

    @Test
    fun `sync rebases and pushes when remote branch advanced`() {
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
        val remoteRoot = initializeGitRemote()
        pushFromPeer(
            remoteRoot = remoteRoot,
            relativePath = "notes.txt",
            contents = "remote update",
            commitMessage = "remote: update notes"
        )

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

        val sync = service.sync("cms: update hello", push = true)

        assertTrue(sync.committed)
        assertTrue(sync.pushSucceeded)
        assertEquals(0, sync.dirtyRemaining)
        assertFalse(service.get("posts/hello.md").dirty)
        assertTrue(sync.message.contains("rebasing onto origin/"))

        Git.cloneRepository().setURI(remoteRoot.toUri().toString()).setDirectory(createScratchDir("statik-cms-verify").toFile()).call().use { clone ->
            val pulled = clone.repository.workTree.toPath()
            assertTrue((pulled / "posts" / "hello.md").readText().contains("Synced content."))
            assertTrue((pulled / "notes.txt").readText().contains("remote update"))
        }
    }

    @Test
    fun `sync keeps CMS entries dirty when push fails after rebase conflict`() {
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
        val remoteRoot = initializeGitRemote()
        pushFromPeer(
            remoteRoot = remoteRoot,
            relativePath = "posts/hello.md",
            contents = """
                ---
                title: Remote
                published: 2024-01-04T12:00:00
                ---
                Remote body.
            """.trimIndent(),
            commitMessage = "remote: update hello"
        )

        val service = CmsService(tempRoot, config, generator).also { it.bootstrap() }
        service.save(
            CmsSaveRequest(
                type = CmsContentType.POST,
                sourcePath = "posts/hello.md",
                frontmatter = """
                    title: Local
                    published: 2024-01-05T12:00:00
                """.trimIndent(),
                body = "Local body."
            )
        )

        val sync = service.sync("cms: conflicting hello", push = true)

        assertTrue(sync.committed)
        assertFalse(sync.pushSucceeded)
        assertEquals(1, sync.dirtyRemaining)
        assertTrue(service.get("posts/hello.md").dirty)
        assertTrue(sync.message.contains("automatic rebase could not be completed"))

        Git.open(tempRoot.toFile()).use { git ->
            assertTrue(git.status().call().isClean)
        }
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

    private fun initializeGitRemote(): java.nio.file.Path {
        val remoteRoot = createScratchDir("statik-cms-remote")
        Git.init().setBare(true).setDirectory(remoteRoot.toFile()).call().use { }

        Git.init().setDirectory(tempRoot.toFile()).call().use { git ->
            git.add().addFilepattern(".").call()
            git.commit()
                .setMessage("init")
                .setAuthor("Init", "init@example.com")
                .setCommitter("Init", "init@example.com")
                .call()
            git.remoteAdd()
                .setName("origin")
                .setUri(URIish(remoteRoot.toUri().toString()))
                .call()

            val branch = git.repository.branch
            git.push()
                .setRemote("origin")
                .setRefSpecs(RefSpec("HEAD:refs/heads/$branch"))
                .call()
            config = config.copy(
                cms = config.cms.copy(
                    git = config.cms.git.copy(branch = branch)
                )
            )
        }

        return remoteRoot
    }

    private fun pushFromPeer(
        remoteRoot: java.nio.file.Path,
        relativePath: String,
        contents: String,
        commitMessage: String
    ) {
        val peerRoot = createScratchDir("statik-cms-peer")
        Git.cloneRepository().setURI(remoteRoot.toUri().toString()).setDirectory(peerRoot.toFile()).call().use { git ->
            val file = peerRoot / relativePath
            file.parent.createDirectories()
            file.writeText(contents)
            git.add().addFilepattern(relativePath).call()
            git.commit()
                .setMessage(commitMessage)
                .setAuthor("Peer", "peer@example.com")
                .setCommitter("Peer", "peer@example.com")
                .call()
            val branch = git.repository.branch
            git.push()
                .setRemote("origin")
                .setRefSpecs(RefSpec("HEAD:refs/heads/$branch"))
                .call()
        }
    }

    private fun createScratchDir(prefix: String): java.nio.file.Path {
        return createTempDirectory(prefix).also(extraRoots::add)
    }
}
