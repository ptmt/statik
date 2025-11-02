package com.potomushto.statik.template

/**
 * Interface for HTML post-processing operations like minification and beautification
 */
interface HtmlProcessor {
    fun process(html: String): String
}

/**
 * No-op processor that returns HTML unchanged
 */
class NoOpHtmlProcessor : HtmlProcessor {
    override fun process(html: String): String = html
}