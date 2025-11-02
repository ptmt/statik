package com.potomushto.statik.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.template.helpers.HandlebarsHelperRegistrar
import com.potomushto.statik.template.helpers.HelperRegistrationContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import java.util.ServiceLoader

private val logger = LoggerFactory.getLogger(HandlebarsTemplateEngine::class.java)
class HandlebarsTemplateEngine(val templatesPath: Path) : TemplateEngine {
    override val extension = "hbs"

    private val handlebars: Handlebars = Handlebars().prettyPrint(true)
    private val layoutCache = mutableMapOf<String, String>()
    private val blockRegistry = ThreadLocal<MutableMap<String, MutableList<CharSequence>>?>()
    private val helperContext = HelperRegistrationContext(templatesPath, blockRegistry)

    fun registerHelper(name: String, helper: Helper<*>) {
        handlebars.registerHelper(name, helper)
    }

    override fun registerPartial(name: String, partial: String) {
        // Partials are loaded via the include helper dynamically
        // No need to pre-register them
    }

    init {
        ServiceLoader.load(HandlebarsHelperRegistrar::class.java, javaClass.classLoader)
            .forEach { registrar ->
                registrar.register(handlebars, helperContext)
            }
    }

    override fun compile(template: String): (Map<String, Any?>) -> String {
        val compiledTemplate = handlebars.compileInline(template)
        return { data -> compiledTemplate.apply(data) }
    }

    override fun render(template: String, data: Map<String, Any?>): String {
        val compiledTemplate = compile(template)
        return compiledTemplate(data)
    }

    /**
     * Load a layout template from the layouts directory
     * Falls back to FallbackTemplates.DEFAULT_LAYOUT if not found
     */
    private fun loadLayout(layoutName: String): String? {
        if (layoutCache.containsKey(layoutName)) {
            return layoutCache[layoutName]
        }

        val layoutPath = templatesPath.resolve("layouts").resolve("$layoutName.$extension")
        val content = if (Files.exists(layoutPath)) {
            layoutPath.readText()
        } else if (layoutName == "default") {
            // Use fallback default layout when user doesn't have one
            logger.debug { "Using fallback default layout from resources" }
            FallbackTemplates.DEFAULT_LAYOUT
        } else {
            null
        }

        if (content != null) {
            layoutCache[layoutName] = content
        }

        return content
    }

    /**
     * Render a template with an optional layout wrapper
     * If layout is specified in data, wraps the template content in the layout
     * Falls back to "default" layout if the specified layout is not found
     */
    override fun renderWithLayout(template: String, data: Map<String, Any?>): String {
        val layoutName = data["layout"] as? String

        val previousBlocks = blockRegistry.get()
        val collectedBlocks = mutableMapOf<String, MutableList<CharSequence>>()
        blockRegistry.set(collectedBlocks)

        val contentHtml = try {
            render(template, data)
        } catch (ex: Exception) {
            recoverPreviousBlocks(previousBlocks)
            throw ex
        }

        // If no layout specified, return content as-is
        if (layoutName == null) {
            recoverPreviousBlocks(previousBlocks)
            return contentHtml
        }

        // Load and apply layout
        var layoutTemplate = loadLayout(layoutName)

        // If layout not found and it's not already "default", try falling back to default
        if (layoutTemplate == null && layoutName != "default") {
            logger.warn("Layout '$layoutName' not found, falling back to 'default' layout")
            layoutTemplate = loadLayout("default")
        }

        if (layoutTemplate != null) {
            // Create new data map with content injected
            val layoutData = data.toMutableMap()
            layoutData["content"] = contentHtml
            val renderedLayout = try {
                render(layoutTemplate, layoutData)
            } finally {
                recoverPreviousBlocks(previousBlocks)
            }
            return renderedLayout
        }

        // If no layout found at all, return content without layout
        recoverPreviousBlocks(previousBlocks)
        return contentHtml
    }

    private fun recoverPreviousBlocks(previous: MutableMap<String, MutableList<CharSequence>>?) {
        if (previous != null) {
            blockRegistry.set(previous)
        } else {
            blockRegistry.remove()
        }
    }
}
