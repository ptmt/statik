package com.potomushto.statik.cms

import com.potomushto.statik.config.CmsAuthConfig
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CmsAuthServiceTest {

    @Test
    fun `oauth callback denies users outside the allowlist`() {
        val service = CmsAuthService(
            config = authConfig(),
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

    private fun authConfig(): CmsAuthConfig {
        return CmsAuthConfig(
            enabled = true,
            allowedUser = "potomushto",
            clientId = "github-client-id",
            clientSecretEnv = "GITHUB_CLIENT_SECRET",
            callbackUrl = "https://cms.example.com/__statik__/cms/auth/github/callback",
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
