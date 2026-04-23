package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.config.CmsAuthConfig
import com.potomushto.statik.config.CmsConfig
import com.potomushto.statik.config.PathConfig
import com.potomushto.statik.config.ThemeConfig
import com.potomushto.statik.generators.SiteGenerator
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class CmsRoutesTest {
    private lateinit var tempRoot: java.nio.file.Path
    private lateinit var config: BlogConfig
    private var now = 1_700_000_000_000L

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDirectory("statik-cms-routes")
        (tempRoot / "posts").createDirectories()
        (tempRoot / "pages").createDirectories()
        (tempRoot / "static").createDirectories()
        (tempRoot / "private-key.pem").writeText("test-private-key")

        config = BlogConfig(
            siteName = "CMS Routes Test",
            baseUrl = "https://example.com",
            description = "CMS route tests",
            author = "Test Author",
            theme = ThemeConfig(templates = "templates", assets = listOf("static"), output = "build"),
            paths = PathConfig(posts = "posts", pages = listOf("pages")),
            cms = CmsConfig(
                enabled = true,
                auth = authConfig()
            )
        )
        now = 1_700_000_000_000L
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `preview redirects to cms login when auth session is missing`() = testApplication {
        val service = createCmsService()
        val auth = createAuthService()

        application {
            routing {
                installCmsRoutes(
                    siteNameProvider = { config.siteName },
                    basePath = CmsService.normalizeBasePath(config.cms.basePath),
                    cmsServiceProvider = { service },
                    authService = auth,
                    json = Json { ignoreUnknownKeys = true }
                )
            }
        }

        val client = createClient {
            followRedirects = false
        }
        val response = client.get("/__statik__/cms/preview/draft")

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/__statik__/cms/login", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `authenticated preview serves draft content`() = testApplication {
        val service = createCmsService()
        val auth = createAuthService()
        val session = createSession(auth)

        application {
            routing {
                installCmsRoutes(
                    siteNameProvider = { config.siteName },
                    basePath = CmsService.normalizeBasePath(config.cms.basePath),
                    cmsServiceProvider = { service },
                    authService = auth,
                    json = Json { ignoreUnknownKeys = true }
                )
            }
        }

        val response = client.get("/__statik__/cms/preview/draft") {
            header(HttpHeaders.Cookie, "statik_cms_session=${session.id}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Draft body."))
    }

    @Test
    fun `preview remains unprotected when cms auth is disabled`() = testApplication {
        val unprotectedConfig = config.copy(
            cms = config.cms.copy(auth = CmsAuthConfig(enabled = false))
        )
        val service = createCmsService(unprotectedConfig)

        application {
            routing {
                installCmsRoutes(
                    siteNameProvider = { unprotectedConfig.siteName },
                    basePath = CmsService.normalizeBasePath(unprotectedConfig.cms.basePath),
                    cmsServiceProvider = { service },
                    authService = null,
                    json = Json { ignoreUnknownKeys = true }
                )
            }
        }

        val response = client.get("/__statik__/cms/preview/draft")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Draft body."))
    }

    private fun createCmsService(serviceConfig: BlogConfig = config): CmsService {
        createPost(
            "posts/draft.md",
            """
                ---
                title: Draft Preview
                published: 2024-01-02T09:30:00
                draft: true
                ---
                Draft body.
            """.trimIndent()
        )

        val generator = SiteGenerator(tempRoot.toString(), serviceConfig)
        generator.generate()
        return CmsService(tempRoot, serviceConfig, generator).also { it.bootstrap() }
    }

    private fun createAuthService(): CmsAuthService {
        return CmsAuthService(
            config = authConfig(),
            hostRoot = tempRoot,
            client = RouteFakeGitHubOAuthClient(login = "potomushto"),
            secretProvider = { "secret" },
            nowProvider = { now }
        )
    }

    private fun createSession(auth: CmsAuthService): CmsAuthSession {
        val authorizationUrl = auth.startAuthorization()
        return auth.completeAuthorization(
            code = "oauth-code",
            state = queryParam(authorizationUrl, "state")
        )
    }

    private fun createPost(path: String, contents: String) {
        val file = tempRoot / path
        file.parent.createDirectories()
        file.writeText(contents)
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
        val query = java.net.URI(url).rawQuery.orEmpty()
        return query.split("&")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == name) {
                    java.net.URLDecoder.decode(parts[1], Charsets.UTF_8)
                } else {
                    null
                }
            }
            .first()
    }
}

private class RouteFakeGitHubOAuthClient(
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
