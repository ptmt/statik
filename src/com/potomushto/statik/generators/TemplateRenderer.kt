package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.logging.LoggerFactory
import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage
import com.potomushto.statik.template.FallbackTemplates
import com.potomushto.statik.template.HandlebarsTemplateEngine
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Responsible for rendering all templates (posts, pages, home)
 */
class TemplateRenderer(
    private val templatesPath: Path,
    private val templateEngine: HandlebarsTemplateEngine,
    private val config: BlogConfig,
    private val baseUrlOverride: String?
) {
    private val logger = LoggerFactory.getLogger(TemplateRenderer::class.java)

    // Use overridden baseUrl if provided, otherwise use config baseUrl
    private val effectiveBaseUrl: String
        get() = baseUrlOverride ?: config.baseUrl

    private fun createRenderTrace(type: String, id: String? = null, additional: Map<String, String> = emptyMap()): String {
        return buildString {
            appendLine("  Type: $type")
            if (id != null) {
                appendLine("  ID: $id")
            }
            additional.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
        }
    }

    /**
     * Render a single blog post to HTML
     */
    fun renderPost(post: BlogPost, allPages: List<SitePage>, datasourceContext: Map<String, Any?>): String {
        val templateContent = getTemplateContent("post", FallbackTemplates.POST_TEMPLATE)

        return if (post.isTemplate) {
            // For template files, use layout if specified in metadata or default
            val layout = post.metadata["layout"] ?: "default"
            val description: String = post.metadata["description"] ?: post.title
            templateEngine.renderWithLayout(
                post.content,
                mapOf(
                    "post" to post,
                    "baseUrl" to effectiveBaseUrl,
                    "siteName" to config.siteName,
                    "pages" to allPages,
                    "title" to post.title,
                    "description" to description,
                    "layout" to layout,
                    "__renderTrace" to createRenderTrace(
                        "Post",
                        post.id,
                        mapOf(
                            "Template" to "Content is HBS template",
                            "Path" to post.path,
                            "Date" to post.date.toString()
                        )
                    )
                ).withDatasource(datasourceContext)
            )
        } else {
            val layout = post.metadata["layout"] ?: "default"
            templateEngine.renderWithLayout(
                templateContent,
                mapOf(
                    "post" to post,
                    "baseUrl" to effectiveBaseUrl,
                    "siteName" to config.siteName,
                    "pages" to allPages,
                    "title" to post.title,
                    "description" to post.content.take(160),
                    "layout" to layout,
                    "__renderTrace" to createRenderTrace(
                        "Post",
                        post.id,
                        mapOf(
                            "Template" to "post.hbs",
                            "Path" to post.path,
                            "Date" to post.date.toString()
                        )
                    )
                ).withDatasource(datasourceContext)
            )
        }
    }

    /**
     * Render a single page to HTML
     */
    fun renderPage(page: SitePage, allPages: List<SitePage>, datasourceContext: Map<String, Any?>): String {
        val templateContent = getTemplateContent("page", FallbackTemplates.PAGE_TEMPLATE)

        return if (page.isTemplate) {
            // For template files, render directly with layout
            val layout = page.metadata["layout"] ?: "default"
            val description: String = page.metadata["description"] ?: config.description
            templateEngine.renderWithLayout(
                page.content,
                mapOf(
                    "page" to page,
                    "pages" to allPages,
                    "baseUrl" to effectiveBaseUrl,
                    "siteName" to config.siteName,
                    "description" to description,
                    "title" to page.title,
                    "layout" to layout,
                    "__renderTrace" to createRenderTrace(
                        "Page",
                        page.id,
                        mapOf(
                            "Template" to "Content is HBS template",
                            "Path" to page.path,
                            "Nav Order" to (page.navOrder?.toString() ?: "none")
                        )
                    )
                ).withDatasource(datasourceContext)
            )
        } else {
            val layout = page.metadata["layout"] ?: "default"
            templateEngine.renderWithLayout(
                templateContent,
                mapOf(
                    "page" to page,
                    "pages" to allPages,
                    "baseUrl" to effectiveBaseUrl,
                    "siteName" to config.siteName,
                    "description" to config.description,
                    "title" to page.title,
                    "layout" to layout,
                    "__renderTrace" to createRenderTrace(
                        "Page",
                        page.id,
                        mapOf(
                            "Template" to "page.hbs",
                            "Path" to page.path,
                            "Nav Order" to (page.navOrder?.toString() ?: "none")
                        )
                    )
                ).withDatasource(datasourceContext)
            )
        }
    }

    /**
     * Render the home page to HTML
     */
    fun renderHomePage(posts: List<BlogPost>, pages: List<SitePage>, datasourceContext: Map<String, Any?>): String {
        val templateContent = getTemplateContent("home", FallbackTemplates.HOME_TEMPLATE)

        return templateEngine.renderWithLayout(
            templateContent,
            mapOf(
                "posts" to posts,
                "siteName" to config.siteName,
                "description" to config.description,
                "baseUrl" to effectiveBaseUrl,
                "pages" to pages,
                "featuredPage" to pages.firstOrNull { it.path.isNotEmpty() },
                "layout" to "default",
                "__renderTrace" to createRenderTrace(
                    "Home",
                    null,
                    mapOf(
                        "Template" to "home.hbs",
                        "Posts Count" to posts.size.toString(),
                        "Pages Count" to pages.size.toString()
                    )
                )
            ).withDatasource(datasourceContext)
        )
    }

    /**
     * Render the posts listing page to HTML
     */
    fun renderPostsPage(posts: List<BlogPost>, pages: List<SitePage>, datasourceContext: Map<String, Any?>, filterTags: String? = null): String {
        val templateContent = getTemplateContent("posts", FallbackTemplates.POSTS_TEMPLATE)

        val filteredPosts = if (filterTags != null) {
            val requestedTags = filterTags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            posts.filter { post ->
                val postTags = post.tags
                requestedTags.any { tag -> postTags.contains(tag) }
            }
        } else {
            posts
        }

        return templateEngine.renderWithLayout(
            templateContent,
            mapOf(
                "posts" to filteredPosts,
                "total" to filteredPosts.size,
                "filterTags" to filterTags,
                "siteName" to config.siteName,
                "title" to "All Posts",
                "description" to "Browse all blog posts",
                "baseUrl" to effectiveBaseUrl,
                "pages" to pages,
                "layout" to "default",
                "__renderTrace" to createRenderTrace(
                    "Posts Listing",
                    null,
                    mapOf(
                        "Template" to "posts.hbs",
                        "Total Posts" to filteredPosts.size.toString(),
                        "Filter Tags" to (filterTags ?: "none")
                    )
                )
            ).withDatasource(datasourceContext)
        )
    }

    /**
     * Gets template content, falling back to built-in template if file doesn't exist
     */
    private fun getTemplateContent(templateName: String, fallbackTemplate: String): String {
        val templateFile = templatesPath.resolve("$templateName.${templateEngine.extension}")
        return if (Files.exists(templateFile)) {
            templateFile.readText()
        } else {
            logger.warn { "Template $templateName.${templateEngine.extension} not found, using built-in fallback" }
            fallbackTemplate
        }
    }

    private fun Map<String, Any?>.withDatasource(datasource: Map<String, Any?>): Map<String, Any?> =
        this + ("datasource" to datasource)
}
