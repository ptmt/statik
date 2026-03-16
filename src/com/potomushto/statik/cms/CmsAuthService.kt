package com.potomushto.statik.cms

import com.potomushto.statik.config.CmsAuthConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class CmsAuthService(
    private val config: CmsAuthConfig,
    private val client: GitHubOAuthClient = DefaultGitHubOAuthClient(),
    private val secretProvider: (String) -> String? = System::getenv
) {
    private val pendingAuthorizations = ConcurrentHashMap<String, PendingAuthorization>()
    private val sessions = ConcurrentHashMap<String, CmsAuthSession>()
    private val secureRandom = SecureRandom()

    init {
        if (config.enabled) {
            validateConfigured()
        }
    }

    fun isEnabled(): Boolean = config.enabled

    fun startAuthorization(): String {
        validateConfigured()
        purgeExpiredEntries()

        val state = randomToken()
        val codeVerifier = randomToken(64)
        pendingAuthorizations[state] = PendingAuthorization(
            codeVerifier = codeVerifier,
            createdAt = System.currentTimeMillis()
        )

        val codeChallenge = codeChallenge(codeVerifier)
        val query = buildQuery(
            "client_id" to requireValue(config.clientId, "cms.auth.clientId"),
            "redirect_uri" to requireValue(config.callbackUrl, "cms.auth.callbackUrl"),
            "scope" to config.scopes.joinToString(" ").ifBlank { "repo" },
            "state" to state,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
            "login" to requireValue(config.allowedUser, "cms.auth.allowedUser")
        )

        return "https://github.com/login/oauth/authorize?$query"
    }

    fun completeAuthorization(code: String, state: String): CmsAuthSession {
        validateConfigured()
        purgeExpiredEntries()

        val pending = pendingAuthorizations.remove(state)
            ?: throw IllegalArgumentException("Invalid or expired GitHub OAuth state")

        val token = client.exchangeCode(
            config = config,
            clientSecret = clientSecret(),
            code = code,
            codeVerifier = pending.codeVerifier
        )
        val user = client.fetchUser(token.accessToken)
        val allowedUser = requireValue(config.allowedUser, "cms.auth.allowedUser")
        if (!user.login.equals(allowedUser, ignoreCase = true)) {
            throw CmsPermissionDeniedException("permissions denied")
        }

        val session = CmsAuthSession(
            id = randomToken(),
            login = user.login,
            accessToken = token.accessToken,
            createdAt = System.currentTimeMillis()
        )
        sessions[session.id] = session
        return session
    }

    fun currentSession(sessionId: String?): CmsAuthSession? {
        if (!config.enabled) return null
        purgeExpiredEntries()
        val session = sessionId?.let { sessions[it] } ?: return null
        val allowedUser = requireValue(config.allowedUser, "cms.auth.allowedUser")
        if (!session.login.equals(allowedUser, ignoreCase = true)) {
            sessions.remove(session.id)
            throw CmsPermissionDeniedException("permissions denied")
        }
        return session
    }

    fun requireSession(sessionId: String?): CmsAuthSession {
        return currentSession(sessionId) ?: throw CmsAuthenticationRequiredException()
    }

    fun clearSession(sessionId: String?) {
        if (sessionId == null) return
        sessions.remove(sessionId)
    }

    fun status(sessionId: String?): CmsAuthStatus {
        val session = currentSession(sessionId)
        return CmsAuthStatus(
            enabled = config.enabled,
            authenticated = session != null,
            viewer = session?.login,
            allowedUser = config.allowedUser
        )
    }

    private fun validateConfigured() {
        requireValue(config.allowedUser, "cms.auth.allowedUser")
        requireValue(config.clientId, "cms.auth.clientId")
        requireValue(config.clientSecretEnv, "cms.auth.clientSecretEnv")
        requireValue(config.callbackUrl, "cms.auth.callbackUrl")
        require(clientSecret().isNotBlank()) {
            "Missing GitHub OAuth client secret in env '${config.clientSecretEnv}'"
        }
    }

    private fun clientSecret(): String {
        val envName = requireValue(config.clientSecretEnv, "cms.auth.clientSecretEnv")
        return secretProvider(envName).orEmpty()
    }

    private fun purgeExpiredEntries() {
        val now = System.currentTimeMillis()
        pendingAuthorizations.entries.removeIf { now - it.value.createdAt > PENDING_TTL_MS }
        sessions.entries.removeIf { now - it.value.createdAt > SESSION_TTL_MS }
    }

    private fun randomToken(bytes: Int = 32): String {
        val buffer = ByteArray(bytes)
        secureRandom.nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    private fun codeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun buildQuery(vararg pairs: Pair<String, String>): String {
        return pairs.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    private fun requireValue(value: String?, name: String): String {
        return value?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required CMS auth setting: $name")
    }

    private data class PendingAuthorization(
        val codeVerifier: String,
        val createdAt: Long
    )

    companion object {
        private const val PENDING_TTL_MS = 10 * 60 * 1000L
        private const val SESSION_TTL_MS = 12 * 60 * 60 * 1000L
    }
}

class CmsAuthenticationRequiredException(message: String = "Authentication required") : RuntimeException(message)

class CmsPermissionDeniedException(message: String = "permissions denied") : RuntimeException(message)

data class CmsAuthSession(
    val id: String,
    val login: String,
    val accessToken: String,
    val createdAt: Long
)

interface GitHubOAuthClient {
    fun exchangeCode(
        config: CmsAuthConfig,
        clientSecret: String,
        code: String,
        codeVerifier: String
    ): GitHubOAuthToken

    fun fetchUser(accessToken: String): GitHubUser
}

data class GitHubOAuthToken(
    val accessToken: String
)

data class GitHubUser(
    val login: String
)

class DefaultGitHubOAuthClient(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : GitHubOAuthClient {
    override fun exchangeCode(
        config: CmsAuthConfig,
        clientSecret: String,
        code: String,
        codeVerifier: String
    ): GitHubOAuthToken {
        val body = listOf(
            "client_id" to requireValue(config.clientId, "cms.auth.clientId"),
            "client_secret" to clientSecret,
            "code" to code,
            "redirect_uri" to requireValue(config.callbackUrl, "cms.auth.callbackUrl"),
            "code_verifier" to codeVerifier
        ).joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

        val request = HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "statik-cms")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val payload = json.parseToJsonElement(response.body()).jsonObject
        payload["error"]?.jsonPrimitive?.contentOrNull?.let { error ->
            val description = payload["error_description"]?.jsonPrimitive?.contentOrNull.orEmpty()
            throw IllegalArgumentException("GitHub OAuth exchange failed: $error $description".trim())
        }

        val accessToken = payload["access_token"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("GitHub OAuth exchange did not return an access token")

        return GitHubOAuthToken(accessToken)
    }

    override fun fetchUser(accessToken: String): GitHubUser {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", "statik-cms")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 400) {
            throw IllegalArgumentException("GitHub user lookup failed with status ${response.statusCode()}")
        }

        val payload = json.parseToJsonElement(response.body()).jsonObject
        val login = payload["login"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("GitHub user lookup did not return a login")

        return GitHubUser(login)
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    private fun requireValue(value: String?, name: String): String {
        return value?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required CMS auth setting: $name")
    }
}
