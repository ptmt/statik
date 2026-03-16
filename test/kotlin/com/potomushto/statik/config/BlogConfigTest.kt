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
                "assets": ["assets"],
                "output": "build"
              },
              "paths": {
                "posts": "articles",
                "pages": ["content"]
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
        assertEquals(false, config.cms.enabled)
        assertEquals("/__statik__/cms", config.cms.basePath)
        assertEquals(".statik/cms.db", config.cms.databasePath)
        assertEquals(false, config.cms.autoSyncOnSave)
        assertEquals(true, config.cms.git.enabled)
        assertEquals("origin", config.cms.git.remote)
        assertEquals(false, config.cms.git.pushOnSync)
        assertEquals(false, config.cms.auth.enabled)
        assertEquals(null, config.cms.auth.allowedUser)
        assertEquals(null, config.cms.auth.appId)
        assertEquals(null, config.cms.auth.appSlug)
        assertEquals(null, config.cms.auth.privateKeyPath)
        assertEquals(null, config.cms.auth.setupUrl)
        assertEquals(listOf("repo"), config.cms.auth.scopes)
        assertEquals(false, config.cms.repo.enabled)
        assertEquals(null, config.cms.repo.owner)
        assertEquals(null, config.cms.repo.name)
        assertEquals(null, config.cms.repo.branch)
        assertEquals(".statik/checkout", config.cms.repo.checkoutDir)
        assertEquals("datasource", config.staticDatasource.outputDir)
        assertEquals("data-collect", config.staticDatasource.collectAttribute)
        assertEquals("images.json", config.staticDatasource.imagesFileName)
        assertEquals("datasource-config.json", config.staticDatasource.configFile)
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
                "imagesFileName": "media.json",
                "configFile": "custom-datasource.json"
              }
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals("feeds", config.staticDatasource.outputDir)
        assertEquals("data-statik", config.staticDatasource.collectAttribute)
        assertEquals("media.json", config.staticDatasource.imagesFileName)
        assertEquals("custom-datasource.json", config.staticDatasource.configFile)
    }

    @Test
    fun `load overrides CMS config when provided`() {
        val configJson = """
            {
              "siteName": "My Blog",
              "baseUrl": "https://example.com",
              "description": "Desc",
              "author": "Author",
              "cms": {
                "enabled": true,
                "basePath": "/editor",
                "databasePath": ".cache/editor.db",
                "autoSyncOnSave": true,
                "git": {
                  "enabled": true,
                  "remote": "upstream",
                  "branch": "main",
                  "pushOnSync": true,
                  "tokenEnv": "GITHUB_TOKEN",
                  "authorName": "Statik CMS",
                  "authorEmail": "cms@example.com"
                },
                "auth": {
                  "enabled": true,
                  "allowedUser": "potomushto",
                  "clientId": "github-client-id",
                  "clientSecretEnv": "GITHUB_CLIENT_SECRET",
                  "callbackUrl": "https://cms.example.com/editor/auth/github/callback",
                  "appId": "123456",
                  "appSlug": "statik-cms",
                  "privateKeyPath": "keys/statik-cms.pem",
                  "setupUrl": "https://cms.example.com/editor/auth/github/setup",
                  "scopes": ["repo", "read:user"]
                },
                "repo": {
                  "enabled": true,
                  "owner": "potomushto",
                  "name": "statik-site",
                  "branch": "main",
                  "checkoutDir": ".cache/checkout"
                }
              }
            }
        """.trimIndent()

        (tempRoot / "config.json").writeText(configJson)

        val config = BlogConfig.load(tempRoot.toString())

        assertEquals(true, config.cms.enabled)
        assertEquals("/editor", config.cms.basePath)
        assertEquals(".cache/editor.db", config.cms.databasePath)
        assertEquals(true, config.cms.autoSyncOnSave)
        assertEquals(true, config.cms.git.enabled)
        assertEquals("upstream", config.cms.git.remote)
        assertEquals("main", config.cms.git.branch)
        assertEquals(true, config.cms.git.pushOnSync)
        assertEquals("GITHUB_TOKEN", config.cms.git.tokenEnv)
        assertEquals("Statik CMS", config.cms.git.authorName)
        assertEquals("cms@example.com", config.cms.git.authorEmail)
        assertEquals(true, config.cms.auth.enabled)
        assertEquals("potomushto", config.cms.auth.allowedUser)
        assertEquals("github-client-id", config.cms.auth.clientId)
        assertEquals("GITHUB_CLIENT_SECRET", config.cms.auth.clientSecretEnv)
        assertEquals("https://cms.example.com/editor/auth/github/callback", config.cms.auth.callbackUrl)
        assertEquals("123456", config.cms.auth.appId)
        assertEquals("statik-cms", config.cms.auth.appSlug)
        assertEquals("keys/statik-cms.pem", config.cms.auth.privateKeyPath)
        assertEquals("https://cms.example.com/editor/auth/github/setup", config.cms.auth.setupUrl)
        assertEquals(listOf("repo", "read:user"), config.cms.auth.scopes)
        assertEquals(true, config.cms.repo.enabled)
        assertEquals("potomushto", config.cms.repo.owner)
        assertEquals("statik-site", config.cms.repo.name)
        assertEquals("main", config.cms.repo.branch)
        assertEquals(".cache/checkout", config.cms.repo.checkoutDir)
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
