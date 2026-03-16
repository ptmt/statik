package com.potomushto.statik.cms

import com.potomushto.statik.config.CmsAuthConfig
import com.potomushto.statik.config.CmsRepoConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class CmsAuthService(
    private val config: CmsAuthConfig,
    private val hostRoot: Path? = null,
    private val client: GitHubOAuthClient = DefaultGitHubOAuthClient(),
    private val appClient: GitHubAppClient = DefaultGitHubAppApiClient(),
    private val secretProvider: (String) -> String? = System::getenv
) {
    private val pendingAuthorizations = ConcurrentHashMap<String, PendingAuthorization>()
    private val pendingInstallations = ConcurrentHashMap<String, String>()
    private val sessions = ConcurrentHashMap<String, CmsAuthSession>()
    private val secureRandom = SecureRandom()
    private val privateKey: PrivateKey by lazy { loadPrivateKey() }

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
            createdAt = System.currentTimeMillis(),
            installationId = null
        )
        sessions[session.id] = session
        return session
    }

    fun startInstallation(sessionId: String): String {
        requireSession(sessionId)
        purgeExpiredEntries()

        val state = randomToken()
        pendingInstallations[state] = sessionId
        return "https://github.com/apps/${requireValue(config.appSlug, "cms.auth.appSlug")}/installations/new?state=${encode(state)}"
    }

    fun completeInstallation(
        sessionId: String,
        state: String,
        installationId: Long,
        repo: CmsRepoConfig
    ): CmsAuthSession {
        val session = requireSession(sessionId)
        val expectedSessionId = pendingInstallations.remove(state)
            ?: throw IllegalArgumentException("Invalid or expired GitHub App installation state")
        require(expectedSessionId == session.id) {
            "Invalid GitHub App installation state"
        }

        val repoInstallationId = appClient.repositoryInstallationId(createAppJwt(), repo)
            ?: throw IllegalArgumentException("GitHub App is not installed on ${repo.fullName()}")
        if (repoInstallationId != installationId) {
            throw CmsPermissionDeniedException("permissions denied")
        }

        return session.copy(installationId = installationId).also { sessions[it.id] = it }
    }

    fun attachInstallationForRepo(sessionId: String, repo: CmsRepoConfig): CmsAuthSession? {
        val session = requireSession(sessionId)
        if (session.installationId != null) {
            return session
        }

        val installationId = appClient.repositoryInstallationId(createAppJwt(), repo) ?: return null
        return session.copy(installationId = installationId).also { sessions[it.id] = it }
    }

    fun createInstallationAccessToken(sessionId: String, repo: CmsRepoConfig): String {
        val session = attachInstallationForRepo(sessionId, repo)
            ?: throw IllegalStateException("GitHub App installation required for ${repo.fullName()}")
        return appClient.createInstallationAccessToken(
            appJwt = createAppJwt(),
            installationId = session.installationId
                ?: throw IllegalStateException("GitHub App installation required for ${repo.fullName()}"),
            repo = repo
        )
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
        requireValue(config.appId, "cms.auth.appId")
        requireValue(config.appSlug, "cms.auth.appSlug")
        requireValue(config.privateKeyPath, "cms.auth.privateKeyPath")
        requireValue(config.setupUrl, "cms.auth.setupUrl")
        require(clientSecret().isNotBlank()) {
            "Missing GitHub App client secret in env '${config.clientSecretEnv}'"
        }
        require(Files.exists(resolvePrivateKeyPath())) {
            "Missing GitHub App private key at ${resolvePrivateKeyPath()}"
        }
    }

    private fun clientSecret(): String {
        val envName = requireValue(config.clientSecretEnv, "cms.auth.clientSecretEnv")
        return secretProvider(envName).orEmpty()
    }

    private fun purgeExpiredEntries() {
        val now = System.currentTimeMillis()
        pendingAuthorizations.entries.removeIf { now - it.value.createdAt > PENDING_TTL_MS }
        pendingInstallations.entries.removeIf { entry ->
            val session = sessions[entry.value]
            session == null || now - session.createdAt > SESSION_TTL_MS
        }
        sessions.entries.removeIf { now - it.value.createdAt > SESSION_TTL_MS }
    }

    private fun loadPrivateKey(): PrivateKey {
        Files.newBufferedReader(resolvePrivateKeyPath()).use { reader ->
            PEMParser(reader).use { parser ->
                val parsed = parser.readObject()
                    ?: throw IllegalArgumentException("GitHub App private key is empty")
                val converter = JcaPEMKeyConverter()
                return when (parsed) {
                    is PEMKeyPair -> converter.getKeyPair(parsed).private
                    is PrivateKeyInfo -> converter.getPrivateKey(parsed)
                    else -> throw IllegalArgumentException("Unsupported GitHub App private key format")
                }
            }
        }
    }

    private fun resolvePrivateKeyPath(): Path {
        val configuredPath = requireValue(config.privateKeyPath, "cms.auth.privateKeyPath")
        val path = Paths.get(configuredPath)
        return if (path.isAbsolute) path else (hostRoot ?: Paths.get(".")).resolve(path).normalize()
    }

    private fun createAppJwt(): String {
        val now = Instant.now().epochSecond
        val header = base64Url("""{"alg":"RS256","typ":"JWT"}""")
        val payload = base64Url(
            Json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("iat", JsonPrimitive(now - 60))
                    put("exp", JsonPrimitive(now + 9 * 60))
                    put("iss", JsonPrimitive(requireValue(config.appId, "cms.auth.appId")))
                }
            )
        )
        val unsigned = "$header.$payload"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(unsigned.toByteArray(StandardCharsets.UTF_8))
        }.sign()
        return "$unsigned.${Base64.getUrlEncoder().withoutPadding().encodeToString(signature)}"
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

    private fun base64Url(value: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
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
    val createdAt: Long,
    val installationId: Long? = null
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

interface GitHubAppClient {
    fun repositoryInstallationId(appJwt: String, repo: CmsRepoConfig): Long?

    fun createInstallationAccessToken(appJwt: String, installationId: Long, repo: CmsRepoConfig): String
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

class DefaultGitHubAppApiClient(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : GitHubAppClient {
    override fun repositoryInstallationId(appJwt: String, repo: CmsRepoConfig): Long? {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/${repo.ownerPart()}/${repo.namePart()}/installation"))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $appJwt")
            .header("User-Agent", "statik-cms")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 404) {
            return null
        }
        if (response.statusCode() >= 400) {
            throw IllegalStateException("GitHub App installation lookup failed with status ${response.statusCode()}")
        }

        val payload = json.parseToJsonElement(response.body()).jsonObject
        return payload["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: throw IllegalStateException("GitHub App installation lookup did not return an installation id")
    }

    override fun createInstallationAccessToken(appJwt: String, installationId: Long, repo: CmsRepoConfig): String {
        val requestBody = json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("repositories", buildJsonArray { add(JsonPrimitive(repo.namePart())) })
            }
        )

        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/app/installations/$installationId/access_tokens"))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $appJwt")
            .header("Content-Type", "application/json")
            .header("User-Agent", "statik-cms")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 400) {
            throw IllegalStateException("GitHub App installation token request failed with status ${response.statusCode()}")
        }

        val payload = json.parseToJsonElement(response.body()).jsonObject
        return payload["token"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("GitHub App installation token response did not contain a token")
    }
}

internal fun CmsRepoConfig.fullName(): String {
    return "${ownerPart()}/${namePart()}"
}

internal fun CmsRepoConfig.ownerPart(): String {
    return owner?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Missing required CMS repo setting: cms.repo.owner")
}

internal fun CmsRepoConfig.namePart(): String {
    return name?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Missing required CMS repo setting: cms.repo.name")
}
