package com.potomushto.statik.config

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalPathApi::class)
class BlogConfigTest {

    private lateinit var tempRoot: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-config-test")
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `load parses config json`() {
        val configJson = """
            {
              "siteName": "My Blog",
              "baseUrl": "https://example.com",
              "description": "Desc",
              "author": "Author",
              "theme": {
                "templates": "tpl",
                "assets": "assets",
                "output": "build"
              },
              "paths": {
                "posts": "articles",
                "pages": "content"
              }
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals("My Blog", config.siteName)
        assertEquals("https://example.com", config.baseUrl)
        assertEquals("build", config.theme.output)
        assertEquals("articles", config.paths.posts)
        assertEquals(3000, config.devServer.port)
        assertEquals("datasource", config.staticDatasource.outputDir)
        assertEquals("data-collect", config.staticDatasource.collectAttribute)
        assertEquals("images.json", config.staticDatasource.imagesFileName)
    }

    @Test
    fun `load overrides dev server port when provided`() {
        val configJson = """
            {
              "siteName": "My Blog",
              "baseUrl": "https://example.com",
              "description": "Desc",
              "author": "Author",
              "devServer": {
                "port": 4100
              }
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals(4100, config.devServer.port)
    }

    @Test
    fun `load overrides static datasource when provided`() {
        val configJson = """
            {
              "siteName": "My Blog",
              "baseUrl": "https://example.com",
              "description": "Desc",
              "author": "Author",
              "staticDatasource": {
                "outputDir": "feeds",
                "collectAttribute": "data-statik",
                "imagesFileName": "media.json"
              }
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals("feeds", config.staticDatasource.outputDir)
        assertEquals("data-statik", config.staticDatasource.collectAttribute)
        assertEquals("media.json", config.staticDatasource.imagesFileName)
    }

    @Test
    fun `load uses default RSS config when not provided`() {
        val configJson = """
            {
              "siteName": "My Blog",
              "baseUrl": "https://example.com",
              "description": "Desc",
              "author": "Author"
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals(true, config.rss.enabled)
        assertEquals("feed.xml", config.rss.fileName)
        assertEquals(null, config.rss.title)
        assertEquals(null, config.rss.description)
        assertEquals("en-us", config.rss.language)
        assertEquals(20, config.rss.maxItems)
        assertEquals(true, config.rss.includeFullContent)
    }

    @Test
    fun `load overrides RSS config when provided`() {
        val configJson = """
            {
              "siteName": "My Blog",
              "baseUrl": "https://example.com",
              "description": "Desc",
              "author": "Author",
              "rss": {
                "enabled": false,
                "fileName": "rss.xml",
                "title": "Custom RSS Title",
                "description": "Custom RSS Description",
                "language": "fr-fr",
                "maxItems": 10,
                "includeFullContent": false
              }
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals(false, config.rss.enabled)
        assertEquals("rss.xml", config.rss.fileName)
        assertEquals("Custom RSS Title", config.rss.title)
        assertEquals("Custom RSS Description", config.rss.description)
        assertEquals("fr-fr", config.rss.language)
        assertEquals(10, config.rss.maxItems)
        assertEquals(false, config.rss.includeFullContent)
    }

    @Test
    fun `load throws when config missing`() {
        assertFailsWith<IllegalArgumentException> {
            BlogConfig.load(tempRoot.toString())
        }
    }
}
