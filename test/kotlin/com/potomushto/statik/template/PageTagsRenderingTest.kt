package com.potomushto.statik.template

import com.potomushto.statik.models.SitePage
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertFalse

class PageTagsRenderingTest {
    @Test
    fun `page tags should render correctly when using page-tags property`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = """
            {{#if page.tags}}
            <div class="tags">
                {{#each page.tags}}
                <span class="tag">{{this}}</span>
                {{/each}}
            </div>
            {{/if}}
        """.trimIndent()

        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("tags" to "kotlin, web, tutorial"),
            outputPath = "test"
        )

        val data = mapOf(
            "page" to page
        )

        val result = engine.render(template, data)
        println("===== RENDERED OUTPUT =====")
        println(result)
        println("===========================")

        // Should contain the actual tag values
        assert(result.contains("kotlin"))
        assert(result.contains("web"))
        assert(result.contains("tutorial"))

        // Should NOT contain Java/Kotlin String/List methods
        assertFalse(result.contains("isEmpty"), "Result should not contain 'isEmpty'")
        assertFalse(result.contains("hashCode"), "Result should not contain 'hashCode'")
        assertFalse(result.contains("[B@"), "Result should not contain byte array representation")
        assertFalse(result.contains("[C@"), "Result should not contain char array representation")
        assertFalse(result.contains("Optional"), "Result should not contain 'Optional'")
    }

    @Test
    fun `page metadata-tags should fail when iterating over string`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        // This is the WRONG way - iterating over metadata.tags (which is a String)
        val template = """
            {{#if page.metadata.tags}}
            <div class="tags">
                {{#each page.metadata.tags}}
                <span class="tag">{{this}}</span>
                {{/each}}
            </div>
            {{/if}}
        """.trimIndent()

        val page = SitePage(
            id = "test",
            title = "Test Page",
            content = "Content",
            metadata = mapOf("tags" to "kotlin, web"),
            outputPath = "test"
        )

        val data = mapOf(
            "page" to page
        )

        val result = engine.render(template, data)
        println("===== WRONG WAY OUTPUT =====")
        println(result)
        println("============================")

        // When iterating over a String, Handlebars iterates over its methods/properties
        // This demonstrates the bug
        assert(result.contains("isEmpty") || result.contains("hashCode") || result.contains("[B@"))
    }
}
