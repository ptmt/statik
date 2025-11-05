package com.potomushto.statik.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.template.helpers.HandlebarsHelperRegistrar
import com.potomushto.statik.template.helpers.HelperRegistrationContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.readText
import java.util.ServiceLoader

private val logger = LoggerFactory.getLogger(HandlebarsTemplateEngine::class.java)
class HandlebarsTemplateEngine(
    val templatesPath: Path,
    private val htmlProcessor: HtmlProcessor = NoOpHtmlProcessor(),
    private val debugEnabled: Boolean = false
) : TemplateEngine {
    override val extension = "hbs"

    private val handlebars: Handlebars = Handlebars().prettyPrint(false)
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
            return injectDebugMetadata(contentHtml, data, null)
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
                val html = render(layoutTemplate, layoutData)
                // Apply HTML processing (minification/beautification)
                val processed = htmlProcessor.process(html)
                // Inject debug metadata after HTML processing
                injectDebugMetadata(processed, data, layoutName)
            } finally {
                recoverPreviousBlocks(previousBlocks)
            }
            return renderedLayout
        }

        // If no layout found at all, return content without layout
        recoverPreviousBlocks(previousBlocks)
        // Apply HTML processing even for content without layout
        return injectDebugMetadata(htmlProcessor.process(contentHtml), data, null)
    }

    private fun recoverPreviousBlocks(previous: MutableMap<String, MutableList<CharSequence>>?) {
        if (previous != null) {
            blockRegistry.set(previous)
        } else {
            blockRegistry.remove()
        }
    }

    /**
     * Inject debug metadata as HTML comment at the bottom of the page
     */
    private fun injectDebugMetadata(html: String, data: Map<String, Any?>, layoutName: String?): String {
        if (!debugEnabled) {
            return html
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val debugComment = buildString {
            appendLine()
            appendLine("<!--")
            appendLine("═══════════════════════════════════════════════════════════════════")
            appendLine("  Statik Debug Information")
            appendLine("  Generated: $timestamp")
            appendLine("═══════════════════════════════════════════════════════════════════")
            appendLine()

            // Render trace
            val renderTrace = data["__renderTrace"] as? String
            if (renderTrace != null) {
                appendLine("Render Trace:")
                appendLine(renderTrace)
                appendLine()
            }

            // Layout information
            appendLine("Layout:")
            appendLine("  Name: ${layoutName ?: "none"}")
            appendLine()

            // Template context (filtered to show only useful data)
            appendLine("Template Context:")
            data.forEach { (key, value) ->
                // Skip internal keys and large objects
                if (key.startsWith("__") || key == "datasource" || key == "content") {
                    appendLine("  $key: [omitted]")
                } else {
                    when (value) {
                        is String -> appendLine("  $key: \"$value\"")
                        is Number -> appendLine("  $key: $value")
                        is Boolean -> appendLine("  $key: $value")
                        is List<*> -> appendLine("  $key: [${value.size} items]")
                        is Map<*, *> -> {
                            appendLine("  $key: {")
                            // Show first level of map
                            value.entries.take(5).forEach { entry ->
                                appendLine("    ${entry.key}: ${summarizeValue(entry.value)}")
                            }
                            if (value.size > 5) {
                                appendLine("    ... (${value.size - 5} more)")
                            }
                            appendLine("  }")
                        }
                        null -> appendLine("  $key: null")
                        else -> appendLine("  $key: [${value::class.simpleName}]")
                    }
                }
            }

            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════════")
            append("-->")
        }

        // Insert before closing </body> tag if exists, otherwise append to end
        val bodyCloseIndex = html.lastIndexOf("</body>", ignoreCase = true)
        return if (bodyCloseIndex != -1) {
            html.substring(0, bodyCloseIndex) + debugComment + "\n" + html.substring(bodyCloseIndex)
        } else {
            html + debugComment
        }
    }

    private fun summarizeValue(value: Any?): String {
        return when (value) {
            is String -> if (value.length > 50) "\"${value.take(50)}...\"" else "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> "[${value.size} items]"
            is Map<*, *> -> "{${value.size} entries}"
            null -> "null"
            else -> "[${value::class.simpleName}]"
        }
    }
}
