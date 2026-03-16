package com.potomushto.statik.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString

@Serializable
data class BlogConfig(
    val siteName: String,
    val baseUrl: String,
    val description: String,
    val author: String,
    val theme: ThemeConfig = ThemeConfig(),
    val paths: PathConfig = PathConfig(),
    val devServer: DevServerConfig = DevServerConfig(),
    val cms: CmsConfig = CmsConfig(),
    val staticDatasource: StaticDatasourceConfig = StaticDatasourceConfig(),
    val rss: RssConfig = RssConfig(),
    val html: HtmlConfig = HtmlConfig(),
    val debug: DebugConfig = DebugConfig()
) {
    companion object {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        fun load(rootPath: String = ".", configFileName: String = "config.json"): BlogConfig {
            val configFile = Paths.get(rootPath, configFileName)
            require(Files.exists(configFile)) {
                """
                    Missing file: $configFileName at $rootPath.
                    Add $configFileName to the root with following structure:
                    ${
                    json.encodeToString(
                        BlogConfig(
                            "site name",
                            "https://my-blog.com",
                            "description",
                            "John Steward"
                        )
                    )
                }
                """.trimIndent()
            }
            return File(configFile.pathString).readText().let {
                json.decodeFromString(it)
            }
        }

    }
}

@Serializable
data class ThemeConfig(
    val templates: String = "templates",
    val assets: List<String> = listOf("assets"),
    val output: String = "build"
)

@Serializable
data class PathConfig(
    val posts: String = "posts",
    val pages: List<String> = listOf("pages")
)

@Serializable
data class DevServerConfig(
    val port: Int = 3000
)

@Serializable
data class CmsConfig(
    val enabled: Boolean = false,
    val basePath: String = "/__statik__/cms",
    val databasePath: String = ".statik/cms.db",
    val autoSyncOnSave: Boolean = false,
    val git: CmsGitConfig = CmsGitConfig(),
    val auth: CmsAuthConfig = CmsAuthConfig()
)

@Serializable
data class CmsGitConfig(
    val enabled: Boolean = true,
    val remote: String = "origin",
    val branch: String? = null,
    val pushOnSync: Boolean = false,
    val tokenEnv: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null
)

@Serializable
data class CmsAuthConfig(
    val enabled: Boolean = false,
    val allowedUser: String? = null,
    val clientId: String? = null,
    val clientSecretEnv: String? = null,
    val callbackUrl: String? = null,
    val scopes: List<String> = listOf("repo")
)

@Serializable
data class StaticDatasourceConfig(
    val enabled: Boolean = true,
    val outputDir: String = "datasource",
    val collectAttribute: String = "data-collect",
    val imagesFileName: String = "images.json",
    val configFile: String? = "datasource-config.json"
)

@Serializable
data class RssConfig(
    val enabled: Boolean = true,
    val fileName: String = "feed.xml",
    val title: String? = null, // Defaults to siteName if not provided
    val description: String? = null, // Defaults to site description if not provided
    val language: String = "en-us",
    val maxItems: Int = 20,
    val includeFullContent: Boolean = true
)

@Serializable
data class FootnotesConfig(
    val display: FootnoteDisplay = FootnoteDisplay.LIST
)

@Serializable
enum class FootnoteDisplay {
    LIST,
    HOVER
}

@Serializable
data class HtmlConfig(
    val format: HtmlFormat = HtmlFormat.DEFAULT,
    val indentSize: Int = 2,
    val footnotes: FootnotesConfig = FootnotesConfig()
)

@Serializable
enum class HtmlFormat {
    DEFAULT,    // No processing, keeps Handlebars prettyPrint output
    MINIFY,     // Minified HTML with no extra whitespace
    BEAUTIFY    // Formatted HTML with proper indentation
}

@Serializable
data class DebugConfig(
    val enabled: Boolean = false,
    val includeMetadata: Boolean = true,
    val includeRenderTrace: Boolean = true
)
