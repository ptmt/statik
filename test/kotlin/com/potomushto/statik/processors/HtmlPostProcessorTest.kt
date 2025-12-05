package com.potomushto.statik.processors

import com.potomushto.statik.config.FootnoteDisplay
import com.potomushto.statik.config.FootnotesConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlPostProcessorTest {
    private val sampleFootnoteHtml = """
        <p>Testing footnote reference<sup id="fnref-1"><a class="footnote-ref" href="#fn-1">1</a></sup> inside text.</p>
        <div class="footnotes">
         <hr>
         <ol>
          <li id="fn-1">
           <p>Sample footnote text with <em>Markdown</em>.</p><a href="#fnref-1" class="footnote-backref">↩</a></li>
         </ol>
        </div>
    """.trimIndent()

    @Test
    fun `hover mode replaces list with inline tooltips`() {
        val processor = HtmlPostProcessor(FootnotesConfig(display = FootnoteDisplay.HOVER))
        val processed = processor.process(sampleFootnoteHtml)
        assertTrue(
            processed.contains("footnote-inline-hover"),
            "Inline class should be present. HTML: $processed"
        )
        assertTrue(processed.contains("[1]"), "Original reference number should remain visible")
        assertTrue(processed.contains("title=\"Sample footnote text with Markdown.\""), "Footnote text should be placed on the tooltip")
        assertFalse(processed.contains("footnotes"), "Original footnote list should be removed")
    }

    @Test
    fun `list mode keeps original markup`() {
        val processor = HtmlPostProcessor(FootnotesConfig(display = FootnoteDisplay.LIST))
        val processed = processor.process(sampleFootnoteHtml)

        assertTrue(processed.contains("footnotes"), "Footnote list should remain when hover mode is disabled")
        assertFalse(processed.contains("footnote-inline-hover"), "No hover class should be injected in list mode")
    }
}
