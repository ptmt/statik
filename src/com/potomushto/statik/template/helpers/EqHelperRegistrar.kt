package com.potomushto.statik.template.helpers

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

class EqHelperRegistrar : HandlebarsHelperRegistrar {
    override fun register(handlebars: Handlebars, context: HelperRegistrationContext) {
        handlebars.registerHelper("eq", Helper<Any> { value, options ->
            val param = options?.param<Any>(0)
            val isEqual = value == param

            // Check tagType to determine if this is a block helper or subexpression
            // SECTION = block helper like {{#eq}}...{{/eq}}
            // VAR = inline/subexpression like {{eq}} or {{#if (eq ...)}}
            val isBlockHelper = options?.tagType?.toString() == "SECTION"

            if (isBlockHelper) {
                // Block helper mode: {{#eq value param}}content{{else}}alt{{/eq}}
                if (isEqual) {
                    options?.fn()
                } else {
                    options?.inverse()
                }
            } else {
                // Inline/subexpression mode: {{#if (eq value param)}}
                isEqual
            }
        })
    }
}
