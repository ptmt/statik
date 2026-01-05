package com.potomushto.statik.generators

import com.potomushto.statik.config.StaticDatasourceConfig
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.metadata.string
import com.potomushto.statik.metadata.toStringMap
import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import com.potomushto.statik.processors.ContentProcessor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class StaticDatasourceGenerator(
    private val rootPath: Path,
    private val outputRoot: Path,
    private val config: StaticDatasourceConfig,
    private val contentProcessor: ContentProcessor
) {
    private val logger = LoggerFactory.getLogger(StaticDatasourceGenerator::class.java)

    private val json = Json { prettyPrint = true }
    private val configJson = Json { ignoreUnknownKeys = true }
    private val datasetDefinitions: List<DatasourceDatasetConfig> by lazy { loadDatasetDefinitions() }

    fun buildBundle(posts: List<BlogPost>, pages: List<SitePage>): StaticDatasourceBundle {
        if (!config.enabled) {
            return StaticDatasourceBundle.EMPTY
        }

        val documents = collectDocuments(posts, pages)

        val images = if (documents.isEmpty()) {
            emptyList()
        } else {
            collectImages(documents)
        }

        val collectables = when {
            documents.isEmpty() -> emptyMap()
            config.collectAttribute.isBlank() -> emptyMap()
            else -> collectCustomEntities(documents, config.collectAttribute.trim())
        }

        val entityDatasets = datasetDefinitions.map { dataset ->
            val fromFolder = collectEntitiesFromFolder(dataset)
            val fromMetadata = collectEntitiesFromMetadata(dataset, posts, pages)
            EntityDatasetResult(
                name = dataset.name,
                output = dataset.output,
                items = (fromFolder + fromMetadata)
            )
        }

        return StaticDatasourceBundle(
            images = images,
            collectables = collectables,
            entityDatasets = entityDatasets
        )
    }

    fun writeBundle(bundle: StaticDatasourceBundle) {
        if (!config.enabled) return
        if (bundle.isEmpty) return

        val datasourceRoot = outputRoot.resolve(config.outputDir)
        datasourceRoot.createDirectories()

        if (bundle.images.isNotEmpty()) {
            val target = datasourceRoot.resolve(config.imagesFileName)
            Files.writeString(target, json.encodeToString(bundle.images))
        }

        bundle.collectables.forEach { (type, items) ->
            if (items.isEmpty()) return@forEach
            val fileName = "${sanitizeType(type)}.json"
            val target = datasourceRoot.resolve(fileName)
            Files.writeString(target, json.encodeToString(items))
        }

        bundle.entityDatasets.forEach { dataset ->
            if (dataset.items.isEmpty()) return@forEach
            val target = datasourceRoot.resolve(dataset.output)
            target.parent?.createDirectories()
            Files.writeString(target, json.encodeToString(dataset.items))
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
                    html = it.content,
                    metadata = it.metadata
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
                    html = it.content,
                    metadata = it.metadata
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

    private fun collectEntitiesFromFolder(dataset: DatasourceDatasetConfig): List<EntityDatasourceItem> {
        val folder = dataset.folder ?: return emptyList()
        val folderPath = rootPath.resolve(folder)
        if (!Files.exists(folderPath)) return emptyList()

        val supportedExtensions = setOf("md", "markdown", "html", "hbs")
        val folderPrefix = folder.replace('\\', '/').trim('/')
        val items = mutableListOf<EntityDatasourceItem>()

        Files.walk(folderPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it.extension.lowercase() in supportedExtensions }
                .filter { it.nameWithoutExtension.lowercase() != "index" }
                .forEach { file ->
                    val parsed = try {
                        contentProcessor.process(file)
                    } catch (e: IllegalArgumentException) {
                        logger.warn { "Skipping unsupported entity file: ${file.toAbsolutePath()} (${e.message})" }
                        return@forEach
                    }

                    val relative = folderPath.relativize(file)
                    val slug = relative.toString()
                        .replace('\\', '/')
                        .substringBeforeLast('.')
                        .ifBlank { file.nameWithoutExtension }

                    val combinedSlug = listOf(folderPrefix, slug)
                        .filter { it.isNotEmpty() }
                        .joinToString("/")

                    val id = parsed.metadata.string("id")?.ifBlank { null }
                        ?: combinedSlug.replace('/', '-').ifBlank { file.nameWithoutExtension }
                    val title = parsed.metadata.string("title")?.ifBlank { null } ?: id

                    val item = EntityDatasourceItem(
                        dataset = dataset.name,
                        id = id,
                        title = title,
                        content = parsed.content,
                        metadata = parsed.metadata.toStringMap(),
                        source = DatasourceItemSource(
                            type = dataset.name,
                            id = id,
                            path = combinedSlug.toDatasourcePath(),
                            title = title
                        )
                    )

                    items.add(item)
                }
        }

        return items
    }

    private fun collectEntitiesFromMetadata(
        dataset: DatasourceDatasetConfig,
        posts: List<BlogPost>,
        pages: List<SitePage>
    ): List<EntityDatasourceItem> {
        val key = dataset.metadataKey?.trim().orEmpty()
        if (key.isEmpty()) return emptyList()

        val expectedValue = dataset.metadataValue?.trim()?.ifEmpty { null }
        val sources = dataset.normalizedSources()
        val items = mutableListOf<EntityDatasourceItem>()

        if (sources.contains(DatasetSource.POSTS)) {
            posts.forEach { post ->
                val metadataValue = post.metadata.string(key) ?: return@forEach
                if (expectedValue == null || metadataValue == expectedValue) {
                    items.add(
                        EntityDatasourceItem(
                            dataset = dataset.name,
                            id = post.metadata.string("id")?.ifBlank { null } ?: post.id,
                            title = post.title,
                            content = post.content,
                            metadata = post.metadata.toStringMap(),
                            source = DatasourceItemSource(
                                type = "post",
                                id = post.id,
                                path = post.path.toUrlPath(),
                                title = post.title
                            )
                        )
                    )
                }
            }
        }

        if (sources.contains(DatasetSource.PAGES)) {
            pages.forEach { page ->
                val metadataValue = page.metadata.string(key) ?: return@forEach
                if (expectedValue == null || metadataValue == expectedValue) {
                    items.add(
                        EntityDatasourceItem(
                            dataset = dataset.name,
                            id = page.metadata.string("id")?.ifBlank { null } ?: page.id,
                            title = page.title,
                            content = page.content,
                            metadata = page.metadata.toStringMap(),
                            source = DatasourceItemSource(
                                type = "page",
                                id = page.id,
                                path = page.path.toUrlPath(),
                                title = page.title
                            )
                        )
                    )
                }
            }
        }

        return items
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

    private fun loadDatasetDefinitions(): List<DatasourceDatasetConfig> {
        val configFileName = config.configFile?.trim()?.ifEmpty { null } ?: return emptyList()
        val configPath = rootPath.resolve(configFileName)
        if (!Files.exists(configPath)) return emptyList()
        return try {
            val content = Files.readString(configPath)
            val parsed = configJson.decodeFromString(DatasourceConfig.serializer(), content)
            parsed.datasets
        } catch (ex: IOException) {
            logger.error("Unable to read ${configPath.toAbsolutePath()}", ex)
            emptyList()
        } catch (ex: Exception) {
            logger.error("Unable to parse ${configPath.toAbsolutePath()}", ex)
            emptyList()
        }
    }

    private fun String.toUrlPath(): String {
        if (isEmpty()) return "/"
        val normalized = if (startsWith("/")) this else "/$this"
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    private fun String.toDatasourcePath(): String {
        if (isEmpty()) return "/"
        val normalized = if (startsWith("/")) this else "/$this"
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    private data class DocumentContext(
        val sourceType: SourceType,
        val id: String,
        val title: String,
        val path: String,
        val html: String,
        val metadata: Map<String, Any?>
    )

    private enum class SourceType(val label: String) {
        POST("post"),
        PAGE("page")
    }

    private enum class DatasetSource {
        POSTS,
        PAGES
    }

    private fun DatasourceDatasetConfig.normalizedSources(): Set<DatasetSource> {
        if (includeSources.isEmpty()) return setOf(DatasetSource.POSTS, DatasetSource.PAGES)
        return includeSources.mapNotNull { value ->
            when (value.lowercase()) {
                "posts" -> DatasetSource.POSTS
                "pages" -> DatasetSource.PAGES
                else -> null
            }
        }.toSet().ifEmpty { setOf(DatasetSource.POSTS, DatasetSource.PAGES) }
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
data class EntityDatasourceItem(
    val dataset: String,
    val id: String,
    val title: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val source: DatasourceItemSource
)

@Serializable
data class DatasourceItemSource(
    val type: String,
    val id: String,
    val path: String,
    val title: String
)

@Serializable
data class DatasourceConfig(
    val datasets: List<DatasourceDatasetConfig> = emptyList()
)

@Serializable
data class DatasourceDatasetConfig(
    val name: String,
    val output: String = "entity-datasource.json",
    val folder: String? = null,
    val metadataKey: String? = null,
    val metadataValue: String? = null,
    val includeSources: List<String> = listOf("posts", "pages")
)

data class EntityDatasetResult(
    val name: String,
    val output: String,
    val items: List<EntityDatasourceItem>
)

data class StaticDatasourceBundle(
    val images: List<ImageDatasourceItem> = emptyList(),
    val collectables: Map<String, List<CollectableDatasourceItem>> = emptyMap(),
    val entityDatasets: List<EntityDatasetResult> = emptyList()
) {
    val isEmpty: Boolean
        get() = images.isEmpty() &&
            collectables.values.all { it.isEmpty() } &&
            entityDatasets.all { it.items.isEmpty() }

    fun toTemplateContext(): Map<String, Any?> = mapOf(
        "images" to images,
        "collectables" to collectables,
        "entities" to entityDatasets.associate { it.name to it.items },
        "datasets" to entityDatasets
    )

    companion object {
        val EMPTY = StaticDatasourceBundle()
    }
}
