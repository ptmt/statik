package com.potomushto.statik.cms

import com.potomushto.statik.config.CmsAuthConfig
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class CmsAuthServiceTest {
    private lateinit var tempRoot: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-cms-auth")
        (tempRoot / "private-key.pem").writeText("test-private-key")
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `oauth callback denies users outside the allowlist`() {
        val service = CmsAuthService(
            config = authConfig(),
            hostRoot = tempRoot,
            client = FakeGitHubOAuthClient(login = "someone-else"),
            secretProvider = { "secret" }
        )

        val state = queryParam(service.startAuthorization(), "state")

        val error = assertFailsWith<CmsPermissionDeniedException> {
            service.completeAuthorization(code = "oauth-code", state = state)
        }

        assertEquals("permissions denied", error.message)
    }

    @Test
    fun `oauth callback creates session for the allowed user`() {
        val service = CmsAuthService(
            config = authConfig(),
            hostRoot = tempRoot,
            client = FakeGitHubOAuthClient(login = "potomushto"),
            secretProvider = { "secret" }
        )

        val authorizationUrl = service.startAuthorization()
        assertEquals("potomushto", queryParam(authorizationUrl, "login"))

        val session = service.completeAuthorization(
            code = "oauth-code",
            state = queryParam(authorizationUrl, "state")
        )

        assertEquals("potomushto", session.login)
        assertNotNull(service.requireSession(session.id))
        assertTrue(service.status(session.id).authenticated)
        assertEquals("potomushto", service.status(session.id).viewer)
    }

    @Test
    fun `installation flow starts from the configured GitHub app slug`() {
        val service = CmsAuthService(
            config = authConfig(),
            hostRoot = tempRoot,
            client = FakeGitHubOAuthClient(login = "potomushto"),
            secretProvider = { "secret" }
        )

        val authorizationUrl = service.startAuthorization()
        val session = service.completeAuthorization(
            code = "oauth-code",
            state = queryParam(authorizationUrl, "state")
        )

        val installationUrl = service.startInstallation(session.id)

        assertTrue(installationUrl.contains("/apps/statik-cms/installations/new"))
        assertTrue(queryParam(installationUrl, "state").isNotBlank())
    }

    @Test
    fun `sessions survive service restart when stored in sqlite`() {
        val databasePath = tempRoot / ".statik" / "cms.db"
        val first = CmsAuthService(
            config = authConfig(),
            hostRoot = tempRoot,
            databasePath = databasePath,
            client = FakeGitHubOAuthClient(login = "potomushto"),
            secretProvider = { "secret" }
        )

        val authorizationUrl = first.startAuthorization()
        val session = first.completeAuthorization(
            code = "oauth-code",
            state = queryParam(authorizationUrl, "state")
        )

        val second = CmsAuthService(
            config = authConfig(),
            hostRoot = tempRoot,
            databasePath = databasePath,
            client = FakeGitHubOAuthClient(login = "potomushto"),
            secretProvider = { "secret" }
        )

        assertEquals("potomushto", second.requireSession(session.id).login)
        assertTrue(second.status(session.id).authenticated)
    }

    private fun authConfig(): CmsAuthConfig {
        return CmsAuthConfig(
            enabled = true,
            allowedUser = "potomushto",
            clientId = "github-client-id",
            clientSecretEnv = "GITHUB_CLIENT_SECRET",
            callbackUrl = "https://cms.example.com/__statik__/cms/auth/github/callback",
            appId = "123456",
            appSlug = "statik-cms",
            privateKeyPath = "private-key.pem",
            setupUrl = "https://cms.example.com/__statik__/cms/auth/github/setup",
            scopes = listOf("repo", "read:user")
        )
    }

    private fun queryParam(url: String, name: String): String {
        val query = URI(url).rawQuery.orEmpty()
        return query.split("&")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == name) java.net.URLDecoder.decode(parts[1], Charsets.UTF_8) else null
            }
            .first()
    }
}

private class FakeGitHubOAuthClient(
    private val login: String
) : GitHubOAuthClient {
    override fun exchangeCode(
        config: CmsAuthConfig,
        clientSecret: String,
        code: String,
        codeVerifier: String
    ): GitHubOAuthToken {
        return GitHubOAuthToken(accessToken = "oauth-token")
    }

    override fun fetchUser(accessToken: String): GitHubUser {
        return GitHubUser(login = login)
    }
}
