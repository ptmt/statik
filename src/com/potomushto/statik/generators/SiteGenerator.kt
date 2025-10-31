package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.processors.MarkdownProcessor
import com.potomushto.statik.processors.ContentProcessor
import com.potomushto.statik.template.HandlebarsTemplateEngine
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Facade for site generation. Delegates to BuildOrchestrator for actual work.
 * Maintains backward compatibility with existing API.
 */
class SiteGenerator(
    private val rootPath: String,
    private val config: BlogConfig,
    private val baseUrlOverride: String? = null
) {
    private val templatesPath = Paths.get(rootPath, config.theme.templates)
    private val markdownProcessor = MarkdownProcessor()
    private val contentProcessor = ContentProcessor(markdownProcessor)
    private val templateEngine = HandlebarsTemplateEngine(templatesPath)
    private val fileWalker = FileWalker(rootPath)

    // Initialize components
    private val contentRepository = ContentRepository(
        rootPath,
        config,
        fileWalker,
        contentProcessor
    )

    private val templateRenderer = TemplateRenderer(
        templatesPath,
        templateEngine,
        config,
        baseUrlOverride
    )

    private val assetManager = AssetManager(
        rootPath,
        config,
        fileWalker
    )

    private val datasourceGenerator = StaticDatasourceGenerator(
        Paths.get(rootPath),
        Paths.get(rootPath, config.theme.output),
        config.staticDatasource,
        contentProcessor
    )

    private val rssGenerator = RssGenerator(
        config,
        Paths.get(rootPath, config.theme.output),
        baseUrlOverride
    )

    private val orchestrator = BuildOrchestrator(
        rootPath,
        config,
        contentRepository,
        templateRenderer,
        assetManager,
        rssGenerator,
        datasourceGenerator
    )

    /**
     * Generate the entire site from scratch
     */
    fun generate() {
        orchestrator.buildFull()
    }

    /**
     * Regenerate only the parts affected by changed files
     */
    fun regenerate(changedFiles: List<Path>) {
        orchestrator.buildIncremental(changedFiles)
    }

    /**
     * Get the content repository for accessing posts and pages
     */
    fun getContentRepository(): ContentRepository = contentRepository
}
