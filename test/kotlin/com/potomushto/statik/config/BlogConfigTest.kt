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
    }

    @Test
    fun `load throws when config missing`() {
        assertFailsWith<IllegalArgumentException> {
            BlogConfig.load(tempRoot.toString())
        }
    }
}

