package com.potomushto.statik.template

/**
 * Lightweight HTML minifier that reduces whitespace and removes unnecessary characters.
 * No external dependencies required - pure Kotlin implementation.
 *
 * Features:
 * - Removes comments (except IE conditional comments)
 * - Collapses whitespace between tags
 * - Preserves whitespace in <pre>, <code>, <textarea>, and <script> tags
 * - Removes leading/trailing whitespace in block elements
 */
class HtmlMinifier : HtmlProcessor {

    private val whitespacePreservingTags = setOf("pre", "code", "textarea", "script", "style")

    override fun process(html: String): String {
        var result = html

        // Remove HTML comments (but preserve IE conditional comments)
        result = result.replace(Regex("<!--(?!\\[if).*?-->", RegexOption.DOT_MATCHES_ALL), "")

        // Process the HTML while respecting whitespace-preserving tags
        result = minifyWithPreservation(result)

        return result
    }

    private fun minifyWithPreservation(html: String): String {
        val result = StringBuilder()
        var currentPos = 0

        // Find all whitespace-preserving tags
        val preserveRegex = Regex("<(${whitespacePreservingTags.joinToString("|")})(?:\\s[^>]*)?>.*?</\\1>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

        val matches = preserveRegex.findAll(html).toList()

        for (match in matches) {
            // Minify the content before this preserved block
            val beforeContent = html.substring(currentPos, match.range.first)
            result.append(minifyContent(beforeContent))

            // Append the preserved content as-is
            result.append(match.value)

            currentPos = match.range.last + 1
        }

        // Minify any remaining content after the last preserved block
        if (currentPos < html.length) {
            result.append(minifyContent(html.substring(currentPos)))
        }

        return result.toString()
    }

    private fun minifyContent(content: String): String {
        var result = content

        // Collapse multiple whitespace characters into a single space
        result = result.replace(Regex("\\s+"), " ")

        // Remove whitespace between tags
        result = result.replace(Regex(">\\s+<"), "><")

        // Remove whitespace at the start of content after opening tag
        result = result.replace(Regex(">\\s+"), ">")

        // Remove whitespace at the end of content before closing tag
        result = result.replace(Regex("\\s+<"), "<")

        return result
    }
}