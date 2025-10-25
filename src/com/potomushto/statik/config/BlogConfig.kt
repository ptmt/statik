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
    val staticDatasource: StaticDatasourceConfig = StaticDatasourceConfig(),
    val rss: RssConfig = RssConfig()
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
    val assets: String = "assets",
    val output: String = "build"
)

@Serializable
data class PathConfig(
    val posts: String = "posts",
    val pages: String = "pages"
)

@Serializable
data class DevServerConfig(
    val port: Int = 3000
)

@Serializable
data class StaticDatasourceConfig(
    val enabled: Boolean = true,
    val outputDir: String = "datasource",
    val collectAttribute: String = "data-collect",
    val imagesFileName: String = "images.json"
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
