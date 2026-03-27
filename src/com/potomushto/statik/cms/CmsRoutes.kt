package com.potomushto.statik.cms

import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json

fun Routing.installCmsRoutes(
    siteNameProvider: () -> String,
    basePath: String,
    cmsServiceProvider: () -> CmsService?,
    authService: CmsAuthService?,
    json: Json,
    workspaceManager: CmsWorkspaceManager? = null
) {
    suspend fun renderCmsShell(call: ApplicationCall) {
        val session = if (authService?.isEnabled() == true) {
            requireHtmlSession(call, basePath, authService) ?: return
        } else {
            null
        }

        val service = resolveCmsService(
            cmsServiceProvider = cmsServiceProvider,
            workspaceManager = workspaceManager,
            authService = authService,
            session = session
        )

        if (workspaceManager != null && service == null) {
            call.respondText(
                CmsWebAssets.installHtml(
                    siteName = siteNameProvider(),
                    basePath = basePath,
                    repositoryName = workspaceManager.repositoryName()
                ),
                ContentType.Text.Html
            )
            return
        }

        if (service == null) {
            throw IllegalStateException("CMS service is not available")
        }

        call.respondText(
            CmsWebAssets.indexHtml(siteNameProvider(), basePath),
            ContentType.Text.Html
        )
    }

    get(basePath) {
        renderCmsShell(call)
    }

    get("$basePath/") {
        renderCmsShell(call)
    }

    get("$basePath/login") {
        if (authService?.isEnabled() != true) {
            call.respondRedirect(basePath)
            return@get
        }

        val existingSession = runCatching {
            authService.currentSession(call.request.cookies[CMS_SESSION_COOKIE])
        }.getOrNull()
        if (existingSession != null) {
            call.respondRedirect(basePath)
            return@get
        }

        call.respondText(CmsWebAssets.loginHtml(siteNameProvider(), basePath), ContentType.Text.Html)
    }

    get("$basePath/install") {
        val auth = requireAuthService(authService)
        val manager = requireManagedWorkspace(workspaceManager)
        val session = requireHtmlSession(call, basePath, auth) ?: return@get

        if (resolveCmsService(cmsServiceProvider, manager, auth, session) != null) {
            call.respondRedirect(basePath)
            return@get
        }

        call.respondRedirect(auth.startInstallation(session.id))
    }

    get("$basePath/auth/github") {
        val auth = requireAuthService(authService)
        call.respondRedirect(auth.startAuthorization())
    }

    get("$basePath/auth/github/callback") {
        val auth = requireAuthService(authService)
        val code = call.request.queryParameters["code"]
            ?: throw IllegalArgumentException("Missing query parameter: code")
        val state = call.request.queryParameters["state"]
            ?: throw IllegalArgumentException("Missing query parameter: state")

        try {
            val session = auth.completeAuthorization(code, state)
            call.response.cookies.append(sessionCookie(basePath, session.id))

            if (workspaceManager != null) {
                resolveCmsService(cmsServiceProvider, workspaceManager, auth, session)
            }

            call.respondRedirect(basePath)
        } catch (_: CmsPermissionDeniedException) {
            call.response.cookies.append(expiredSessionCookie(basePath))
            call.respondText("permissions denied", status = HttpStatusCode.Forbidden)
        }
    }

    get("$basePath/auth/github/setup") {
        val auth = requireAuthService(authService)
        val manager = requireManagedWorkspace(workspaceManager)
        val session = requireHtmlSession(call, basePath, auth) ?: return@get
        val state = call.request.queryParameters["state"]
            ?: throw IllegalArgumentException("Missing query parameter: state")
        val installationId = call.request.queryParameters["installation_id"]?.toLongOrNull()
            ?: throw IllegalArgumentException("Missing or invalid query parameter: installation_id")

        manager.completeInstallation(auth, session, state, installationId)
        call.respondRedirect(basePath)
    }

    post("$basePath/logout") {
        authService?.clearSession(call.request.cookies[CMS_SESSION_COOKIE])
        call.response.cookies.append(expiredSessionCookie(basePath))
        call.respondText("logged out", ContentType.Text.Plain)
    }

    get("$basePath/styles.css") {
        call.respondText(CmsWebAssets.stylesCss, ContentType.Text.CSS)
    }

    get("$basePath/app.js") {
        call.respondText(CmsWebAssets.appJs, ContentType.Text.JavaScript)
    }

    get("$basePath/api/status") {
        val session = requireApiSession(call, basePath, authService) ?: return@get
        val authStatus = authService?.status(session?.id)

        if (workspaceManager != null) {
            resolveCmsService(cmsServiceProvider, workspaceManager, authService, session)
            call.respondJson(json, workspaceManager.status(authStatus))
            return@get
        }

        val service = cmsServiceProvider()
            ?: throw IllegalStateException("CMS service is not available")
        call.respondJson(json, service.status().copy(auth = authStatus))
    }

    get("$basePath/api/content") {
        val session = requireApiSession(call, basePath, authService) ?: return@get
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@get
        val type = CmsContentType.fromString(call.request.queryParameters["type"])
        call.respondJson(json, service.list(type))
    }

    get("$basePath/api/content/item") {
        val session = requireApiSession(call, basePath, authService) ?: return@get
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@get
        val sourcePath = call.request.queryParameters["sourcePath"]
            ?: throw IllegalArgumentException("Missing query parameter: sourcePath")
        call.respondJson(json, service.get(sourcePath))
    }

    get("$basePath/api/media") {
        val session = requireApiSession(call, basePath, authService) ?: return@get
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@get
        call.respondJson(json, service.listMedia())
    }

    post("$basePath/api/content") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@post
        val payload = json.decodeFromString<CmsSaveRequest>(call.receiveText())
        call.respondJson(json, service.save(payload, accessToken(authService, workspaceManager, session)))
    }

    post("$basePath/api/media/upload") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@post
        val payload = json.decodeFromString<CmsMediaUploadRequest>(call.receiveText())
        call.respondJson(json, service.uploadMedia(payload))
    }

    post("$basePath/api/media/rename") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@post
        val payload = json.decodeFromString<CmsMediaRenameRequest>(call.receiveText())
        call.respondJson(json, service.renameMedia(payload))
    }

    post("$basePath/api/media/delete") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@post
        val payload = json.decodeFromString<CmsMediaDeleteRequest>(call.receiveText())
        call.respondJson(json, service.deleteMedia(payload))
    }

    post("$basePath/api/refresh") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@post
        call.respondJson(json, service.refreshIndex())
    }

    post("$basePath/api/sync") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val service = requireCmsService(call, cmsServiceProvider, workspaceManager, authService, session) ?: return@post
        val payload = json.decodeFromString<CmsSyncRequest>(call.receiveText())
        call.respondJson(
            json,
            service.sync(
                commitMessage = payload.commitMessage,
                push = payload.push,
                accessToken = accessToken(authService, workspaceManager, session)
            )
        )
    }
}

private fun resolveCmsService(
    cmsServiceProvider: () -> CmsService?,
    workspaceManager: CmsWorkspaceManager?,
    authService: CmsAuthService?,
    session: CmsAuthSession?
): CmsService? {
    workspaceManager?.serviceOrNull()?.let { return it }

    if (workspaceManager != null && authService != null && session != null) {
        if (workspaceManager.ensureReady(authService, session)) {
            return workspaceManager.serviceOrNull()
        }
        return null
    }

    return cmsServiceProvider()
}

private suspend fun requireCmsService(
    call: ApplicationCall,
    cmsServiceProvider: () -> CmsService?,
    workspaceManager: CmsWorkspaceManager?,
    authService: CmsAuthService?,
    session: CmsAuthSession?
): CmsService? {
    val service = resolveCmsService(cmsServiceProvider, workspaceManager, authService, session)
    if (service != null) {
        return service
    }

    val message = if (workspaceManager != null) {
        "GitHub App installation required for ${workspaceManager.repositoryName()}"
    } else {
        "CMS service is not available"
    }
    call.respondText(message, status = HttpStatusCode.Conflict)
    return null
}

private fun accessToken(
    authService: CmsAuthService?,
    workspaceManager: CmsWorkspaceManager?,
    session: CmsAuthSession?
): String? {
    if (session == null) {
        return null
    }
    return if (workspaceManager != null && authService != null) {
        workspaceManager.createAccessToken(authService, session)
    } else {
        session.accessToken
    }
}

private suspend fun requireHtmlSession(
    call: ApplicationCall,
    basePath: String,
    authService: CmsAuthService
): CmsAuthSession? {
    return try {
        authService.requireSession(call.request.cookies[CMS_SESSION_COOKIE])
    } catch (_: CmsAuthenticationRequiredException) {
        call.respondRedirect("$basePath/login")
        null
    } catch (_: CmsPermissionDeniedException) {
        call.response.cookies.append(expiredSessionCookie(basePath))
        call.respondText("permissions denied", status = HttpStatusCode.Forbidden)
        null
    }
}

private suspend fun requireApiSession(
    call: ApplicationCall,
    basePath: String,
    authService: CmsAuthService?
): CmsAuthSession? {
    if (authService?.isEnabled() != true) {
        return null
    }

    return try {
        authService.requireSession(call.request.cookies[CMS_SESSION_COOKIE])
    } catch (_: CmsAuthenticationRequiredException) {
        call.respondText("Authentication required", status = HttpStatusCode.Unauthorized)
        null
    } catch (_: CmsPermissionDeniedException) {
        call.response.cookies.append(expiredSessionCookie(basePath))
        call.respondText("permissions denied", status = HttpStatusCode.Forbidden)
        null
    }
}

private fun requireAuthService(authService: CmsAuthService?): CmsAuthService {
    return authService?.takeIf { it.isEnabled() }
        ?: throw IllegalArgumentException("CMS auth is not enabled")
}

private fun requireManagedWorkspace(workspaceManager: CmsWorkspaceManager?): CmsWorkspaceManager {
    return workspaceManager
        ?: throw IllegalArgumentException("CMS managed checkout is not enabled")
}

private fun sessionCookie(basePath: String, sessionId: String): Cookie {
    return Cookie(
        name = CMS_SESSION_COOKIE,
        value = sessionId,
        path = basePath,
        httpOnly = true,
        extensions = mapOf("SameSite" to "Lax")
    )
}

private fun expiredSessionCookie(basePath: String): Cookie {
    return Cookie(
        name = CMS_SESSION_COOKIE,
        value = "",
        path = basePath,
        httpOnly = true,
        maxAge = 0,
        extensions = mapOf("SameSite" to "Lax")
    )
}

private suspend inline fun <reified T> ApplicationCall.respondJson(json: Json, value: T) {
    respondText(
        text = json.encodeToString(value),
        contentType = ContentType.Application.Json
    )
}

private const val CMS_SESSION_COOKIE = "statik_cms_session"
