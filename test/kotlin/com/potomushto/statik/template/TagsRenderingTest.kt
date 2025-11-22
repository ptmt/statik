package com.potomushto.statik.template

import com.potomushto.statik.models.BlogPost
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.test.assertFalse

class TagsRenderingTest {
    @Test
    fun `tags from BlogPost should render correctly`() {
        val templatesPath = Files.createTempDirectory("templates")
        val engine = HandlebarsTemplateEngine(templatesPath)

        val template = """
            Tags:
            {{#each post.tags}}
            {{this}}
            {{/each}}
        """.trimIndent()

        val post = BlogPost(
            id = "test",
            title = "Test Post",
            date = LocalDateTime.now(),
            content = "Content",
            metadata = mapOf("tags" to "kotlin, web, tutorial"),
            outputPath = "test"
        )

        val data = mapOf(
            "post" to post
        )

        val result = engine.render(template, data)
        println("===== RENDERED OUTPUT =====")
        println(result)
        println("===========================")

        // Should contain the actual tag values
        assert(result.contains("kotlin"))
        assert(result.contains("web"))
        assert(result.contains("tutorial"))

        // Should NOT contain Java/Kotlin List methods
        assertFalse(result.contains("isEmpty"), "Result should not contain 'isEmpty'")
        assertFalse(result.contains("hashCode"), "Result should not contain 'hashCode'")
        assertFalse(result.contains("[B@"), "Result should not contain byte array representation")
        assertFalse(result.contains("[C@"), "Result should not contain char array representation")
    }
}
