package com.potomushto.statik.generators

import java.nio.file.Files
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

@OptIn(ExperimentalPathApi::class)
class FileWalkerTest {

    private lateinit var tempRoot: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-filewalker-test")
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `walkMarkdownFiles returns markdown and html files`() {
        val postsDir = tempRoot / "posts"
        postsDir.createDirectories()

        (postsDir / "entry.md").writeText("# Markdown file")
        (postsDir / "note.html").writeText("<p>HTML content</p>")
        (postsDir / "ignored.txt").writeText("should be ignored")

        val walker = FileWalker(tempRoot.toString())
        val files = walker.walkMarkdownFiles("posts").map { it.fileName.toString() }.toSet()

        assertEquals(setOf("entry.md", "note.html"), files)
    }

    @Test
    fun `generatePath handles index stripping correctly`() {
        val pagesDir = tempRoot / "pages"
        (pagesDir / "docs").createDirectories()
        val index = Files.createFile(pagesDir / "index.md")
        val nestedIndex = Files.createFile(pagesDir / "docs" / "index.md")
        val simplePage = Files.createFile(pagesDir / "about.md")

        val walker = FileWalker(tempRoot.toString())

        assertEquals("", walker.generatePath(index, "pages", stripIndex = true))
        assertEquals("docs", walker.generatePath(nestedIndex, "pages", stripIndex = true))
        assertEquals("about", walker.generatePath(simplePage, "pages", stripIndex = true))
        assertEquals("docs/index", walker.generatePath(nestedIndex, "pages", stripIndex = false))
    }
}
