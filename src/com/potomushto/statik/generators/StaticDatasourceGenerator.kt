package com.potomushto.statik.generators

import com.potomushto.statik.config.StaticDatasourceConfig
import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

class StaticDatasourceGenerator(
    private val outputRoot: Path,
    private val config: StaticDatasourceConfig
) {

    private val json = Json { prettyPrint = true }

    fun generate(posts: List<BlogPost>, pages: List<SitePage>) {
        if (!config.enabled) return

        val documents = collectDocuments(posts, pages)
        if (documents.isEmpty()) return

        val datasourceRoot = outputRoot.resolve(config.outputDir)
        datasourceRoot.createDirectories()

        val images = collectImages(documents)
        if (images.isNotEmpty()) {
            val target = datasourceRoot.resolve(config.imagesFileName)
            target.parent?.createDirectories()
            Files.writeString(target, json.encodeToString(images))
        }

        val customAttribute = config.collectAttribute.trim()
        if (customAttribute.isNotEmpty()) {
            val collectables = collectCustomEntities(documents, customAttribute)
            collectables.forEach { (type, items) ->
                if (items.isEmpty()) return@forEach
                val fileName = "${sanitizeType(type)}.json"
                val target = datasourceRoot.resolve(fileName)
                target.parent?.createDirectories()
                Files.writeString(target, json.encodeToString(items))
            }
        }
    }

    private fun collectDocuments(posts: List<BlogPost>, pages: List<SitePage>): List<DocumentContext> {
        val postDocs = posts
            .filter { !it.isTemplate }
            .map {
                DocumentContext(
                    sourceType = SourceType.POST,
                    id = it.id,
                    title = it.title,
                    path = it.path,
                    html = it.content
                )
            }

        val pageDocs = pages
            .filter { !it.isTemplate }
            .map {
                DocumentContext(
                    sourceType = SourceType.PAGE,
                    id = it.id,
                    title = it.title,
                    path = it.path,
                    html = it.content
                )
            }

        return postDocs + pageDocs
    }

    private fun collectImages(documents: List<DocumentContext>): List<ImageDatasourceItem> {
        return documents.flatMap { document ->
            val parsed = Jsoup.parse(document.html)
            parsed.select("img[src]").mapNotNull { element ->
                val src = element.attr("src").trim()
                if (src.isEmpty()) {
                    null
                } else {
                    ImageDatasourceItem(
                        src = src,
                        alt = element.attr("alt").takeIf { it.isNotBlank() },
                        title = element.attr("title").takeIf { it.isNotBlank() },
                        source = document.toSource()
                    )
                }
            }
        }
    }

    private fun collectCustomEntities(
        documents: List<DocumentContext>,
        attribute: String
    ): Map<String, List<CollectableDatasourceItem>> {
        val byType = linkedMapOf<String, MutableList<CollectableDatasourceItem>>()

        documents.forEach { document ->
            val parsed = Jsoup.parse(document.html)
            parsed.select("*[${attribute}]").forEach { element ->
                val type = element.attr(attribute).trim()
                if (type.isEmpty()) return@forEach

                val attributes = element
                    .attributes()
                    .associate { attr -> attr.key to attr.value }
                    .filter { (key, _) ->
                        key != attribute && key.startsWith("data-")
                    }

                val item = CollectableDatasourceItem(
                    source = document.toSource(),
                    html = element.outerHtml(),
                    text = element.text().trim().takeIf { it.isNotEmpty() },
                    attributes = attributes
                )

                val list = byType.getOrPut(type) { mutableListOf() }
                list.add(item)
            }
        }

        return byType
    }

    private fun sanitizeType(type: String): String {
        val normalized = type.lowercase().replace("[^a-z0-9-_]".toRegex(), "-")
        val collapsed = normalized.replace("-+".toRegex(), "-").trim('-')
        return if (collapsed.isNotEmpty()) collapsed else "collectable"
    }

    private fun DocumentContext.toSource(): DatasourceItemSource = DatasourceItemSource(
        type = sourceType.label,
        id = id,
        path = path.toUrlPath(),
        title = title
    )

    private fun String.toUrlPath(): String {
        if (isEmpty()) return "/"
        val normalized = if (startsWith("/")) this else "/$this"
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    private data class DocumentContext(
        val sourceType: SourceType,
        val id: String,
        val title: String,
        val path: String,
        val html: String
    )

    private enum class SourceType(val label: String) {
        POST("post"),
        PAGE("page")
    }
}

@Serializable
data class ImageDatasourceItem(
    val src: String,
    val alt: String? = null,
    val title: String? = null,
    val source: DatasourceItemSource
)

@Serializable
data class CollectableDatasourceItem(
    val source: DatasourceItemSource,
    val html: String,
    val text: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

@Serializable
data class DatasourceItemSource(
    val type: String,
    val id: String,
    val path: String,
    val title: String
)
