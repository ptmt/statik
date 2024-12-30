package com.potomushto.statik

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.SiteGenerator


class BlogEngine {
    companion object {
        fun run(path: String) {
            val config = BlogConfig.load(path)
            val generator = SiteGenerator(path, config)
            generator.generate()
        }
    }
}