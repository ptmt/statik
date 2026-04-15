package com.potomushto.statik.cms

import kotlinx.serialization.Serializable

@Serializable
enum class CmsContentType {
    POST,
    PAGE;

    companion object {
        fun fromString(value: String?): CmsContentType? {
            if (value == null) return null
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class CmsContentEntry(
    val type: CmsContentType,
    val sourcePath: String,
    val outputPath: String,
    val title: String,
    val frontmatter: String,
    val body: String,
    val extension: String,
    val metadata: Map<String, Any?>,
    val publishedAt: String?,
    val dirty: Boolean,
    val updatedAt: Long,
    val lastSyncedAt: Long?
)

data class CmsMediaEntry(
    val sourcePath: String,
    val rootPath: String,
    val fileName: String,
    val size: Long,
    val contentType: String?,
    val dirty: Boolean,
    val deleted: Boolean,
    val updatedAt: Long,
    val lastSyncedAt: Long?
)

@Serializable
data class CmsContentSummary(
    val type: CmsContentType,
    val sourcePath: String,
    val outputPath: String,
    val title: String,
    val extension: String,
    val publishedAt: String? = null,
    val navOrder: Int? = null,
    val isDraft: Boolean = false,
    val dirty: Boolean,
    val updatedAt: Long
)

@Serializable
data class CmsContentDocument(
    val type: CmsContentType,
    val sourcePath: String,
    val outputPath: String,
    val title: String,
    val extension: String,
    val frontmatter: String,
    val body: String,
    val publishedAt: String? = null,
    val navOrder: Int? = null,
    val isDraft: Boolean = false,
    val dirty: Boolean,
    val updatedAt: Long,
    val lastSyncedAt: Long? = null
)

@Serializable
data class CmsContentListResponse(
    val items: List<CmsContentSummary>,
    val total: Int,
    val dirty: Int
)

@Serializable
data class CmsMediaItem(
    val sourcePath: String,
    val rootPath: String,
    val fileName: String,
    val size: Long,
    val contentType: String? = null,
    val publicPath: String,
    val isImage: Boolean = false,
    val dirty: Boolean,
    val updatedAt: Long
)

@Serializable
data class CmsMediaListResponse(
    val items: List<CmsMediaItem>,
    val roots: List<String>,
    val total: Int,
    val dirty: Int
)

@Serializable
data class CmsSaveRequest(
    val type: CmsContentType,
    val sourcePath: String,
    val previousSourcePath: String? = null,
    val frontmatter: String = "",
    val body: String = "",
    val sync: Boolean = false,
    val commitMessage: String? = null
)

@Serializable
data class CmsSaveResponse(
    val item: CmsContentDocument,
    val sync: CmsSyncResponse? = null
)

@Serializable
data class CmsSyncRequest(
    val commitMessage: String? = null,
    val push: Boolean? = null
)

@Serializable
data class CmsSyncResponse(
    val committed: Boolean,
    val commitId: String? = null,
    val message: String,
    val pushAttempted: Boolean,
    val pushSucceeded: Boolean,
    val files: List<String>,
    val dirtyRemaining: Int
)

@Serializable
data class CmsGitStatus(
    val available: Boolean,
    val enabled: Boolean,
    val repoRoot: String? = null,
    val branch: String? = null,
    val remote: String? = null,
    val remoteUrl: String? = null
)

@Serializable
data class CmsAuthStatus(
    val enabled: Boolean,
    val authenticated: Boolean,
    val viewer: String? = null,
    val allowedUser: String? = null
)

@Serializable
data class CmsStatusResponse(
    val enabled: Boolean,
    val basePath: String,
    val ready: Boolean = true,
    val repository: String? = null,
    val items: Int,
    val dirty: Int,
    val lastSyncedAt: Long? = null,
    val git: CmsGitStatus,
    val auth: CmsAuthStatus? = null
)

@Serializable
data class CmsRefreshResponse(
    val items: Int,
    val dirty: Int,
    val message: String? = null
)

@Serializable
data class CmsMediaUploadRequest(
    val targetDirectory: String,
    val fileName: String,
    val contentsBase64: String
)

@Serializable
data class CmsMediaRenameRequest(
    val sourcePath: String,
    val targetPath: String
)

@Serializable
data class CmsMediaDeleteRequest(
    val sourcePath: String
)

@Serializable
data class CmsMediaMutationResponse(
    val message: String,
    val selectedPath: String? = null,
    val affectedPaths: List<String> = emptyList()
)

internal fun CmsContentEntry.toSummary(): CmsContentSummary {
    return CmsContentSummary(
        type = type,
        sourcePath = sourcePath,
        outputPath = outputPath,
        title = title,
        extension = extension,
        publishedAt = publishedAt,
        navOrder = metadata.cmsNavOrder(),
        isDraft = metadata.cmsIsDraft(),
        dirty = dirty,
        updatedAt = updatedAt
    )
}

internal fun CmsContentEntry.toDocument(): CmsContentDocument {
    return CmsContentDocument(
        type = type,
        sourcePath = sourcePath,
        outputPath = outputPath,
        title = title,
        extension = extension,
        frontmatter = frontmatter,
        body = body,
        publishedAt = publishedAt,
        navOrder = metadata.cmsNavOrder(),
        isDraft = metadata.cmsIsDraft(),
        dirty = dirty,
        updatedAt = updatedAt,
        lastSyncedAt = lastSyncedAt
    )
}

internal fun CmsMediaEntry.toItem(publicPath: String): CmsMediaItem {
    return CmsMediaItem(
        sourcePath = sourcePath,
        rootPath = rootPath,
        fileName = fileName,
        size = size,
        contentType = contentType,
        publicPath = publicPath,
        isImage = contentType?.startsWith("image/") == true,
        dirty = dirty,
        updatedAt = updatedAt
    )
}

private fun Map<String, Any?>.cmsIsDraft(): Boolean {
    return this["draft"]?.toString()?.lowercase() in setOf("true", "yes", "1")
}

private fun Map<String, Any?>.cmsNavOrder(): Int? {
    return this["nav_order"]?.toString()?.toIntOrNull()
        ?: this["navOrder"]?.toString()?.toIntOrNull()
}
