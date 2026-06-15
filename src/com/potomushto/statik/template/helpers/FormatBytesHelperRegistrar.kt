package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

class FormatBytesHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("formatBytes", Helper<Any> { value, _ ->
            val size = when (value) {
                is Number -> value.toDouble()
                is CharSequence -> value.toString().toDoubleOrNull()
                else -> null
            }

            when {
                size == null || !size.isFinite() || size <= 0.0 -> ""
                size < 1024.0 -> "${size.toLong()} B"
                size < 1024.0 * 1024.0 -> "${formatDecimal(size / 1024.0)} KB"
                else -> "${formatDecimal(size / (1024.0 * 1024.0))} MB"
            }
        })
    }

    private fun formatDecimal(value: Double): String {
        return "%.1f".format(java.util.Locale.US, value).removeSuffix(".0")
    }
}
