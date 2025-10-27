package com.potomushto.statik.generators

import com.potomushto.statik.models.BlogPost
import com.potomushto.statik.models.SitePage

/**
 * Holds the complete state for a site build
 */
data class BuildContext(
    val posts: List<BlogPost>,
    val pages: List<SitePage>,
    val datasourceContext: Map<String, Any?>
)