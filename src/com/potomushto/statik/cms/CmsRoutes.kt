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
    siteName: String,
    basePath: String,
    cmsService: CmsService,
    authService: CmsAuthService?,
    json: Json
) {
    get(basePath) {
        if (authService?.isEnabled() == true) {
            if (!ensureHtmlSession(call, basePath, authService)) return@get
        }
        call.respondText(CmsWebAssets.indexHtml(siteName, basePath), ContentType.Text.Html)
    }

    get("$basePath/") {
        if (authService?.isEnabled() == true) {
            if (!ensureHtmlSession(call, basePath, authService)) return@get
        }
        call.respondText(CmsWebAssets.indexHtml(siteName, basePath), ContentType.Text.Html)
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

        call.respondText(CmsWebAssets.loginHtml(siteName, basePath), ContentType.Text.Html)
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
            call.respondRedirect(basePath)
        } catch (_: CmsPermissionDeniedException) {
            call.response.cookies.append(expiredSessionCookie(basePath))
            call.respondText("permissions denied", status = HttpStatusCode.Forbidden)
        }
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
        val status = cmsService.status().copy(
            auth = authService?.status(session?.id)
        )
        call.respondJson(json, status)
    }

    get("$basePath/api/content") {
        requireApiSession(call, basePath, authService) ?: return@get
        val type = CmsContentType.fromString(call.request.queryParameters["type"])
        call.respondJson(json, cmsService.list(type))
    }

    get("$basePath/api/content/item") {
        requireApiSession(call, basePath, authService) ?: return@get
        val sourcePath = call.request.queryParameters["sourcePath"]
            ?: throw IllegalArgumentException("Missing query parameter: sourcePath")
        call.respondJson(json, cmsService.get(sourcePath))
    }

    post("$basePath/api/content") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val payload = json.decodeFromString<CmsSaveRequest>(call.receiveText())
        call.respondJson(json, cmsService.save(payload, session?.accessToken))
    }

    post("$basePath/api/refresh") {
        requireApiSession(call, basePath, authService) ?: return@post
        call.respondJson(json, cmsService.refreshIndex())
    }

    post("$basePath/api/sync") {
        val session = requireApiSession(call, basePath, authService) ?: return@post
        val payload = json.decodeFromString<CmsSyncRequest>(call.receiveText())
        call.respondJson(json, cmsService.sync(payload.commitMessage, payload.push, session?.accessToken))
    }
}

private suspend fun ensureHtmlSession(
    call: ApplicationCall,
    basePath: String,
    authService: CmsAuthService
): Boolean {
    return try {
        authService.requireSession(call.request.cookies[CMS_SESSION_COOKIE])
        true
    } catch (_: CmsAuthenticationRequiredException) {
        call.respondRedirect("$basePath/login")
        false
    } catch (_: CmsPermissionDeniedException) {
        call.response.cookies.append(expiredSessionCookie(basePath))
        call.respondText("permissions denied", status = HttpStatusCode.Forbidden)
        false
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
