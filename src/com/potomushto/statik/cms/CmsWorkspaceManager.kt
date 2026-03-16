package com.potomushto.statik.cms

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.SiteGenerator
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CmsWorkspaceManager(
    private val hostRoot: Path,
    private val bootstrapConfig: BlogConfig
) {
    private val lock = Any()
    private val repoConfig = bootstrapConfig.cms.repo
    private val checkoutRoot = resolveCheckoutRoot(hostRoot, repoConfig.checkoutDir)
    private val databasePath = resolveDatabasePath(hostRoot, bootstrapConfig.cms.databasePath)

    @Volatile
    private var siteConfig: BlogConfig? = null

    @Volatile
    private var cmsService: CmsService? = null

    init {
        if (repoConfig.enabled && Files.exists(checkoutRoot.resolve("config.json"))) {
            synchronized(lock) {
                initializeWorkspace(generate = true)
            }
        }
    }

    fun isManagedRepo(): Boolean = repoConfig.enabled

    fun siteName(): String = siteConfig?.siteName ?: bootstrapConfig.siteName

    fun repositoryName(): String = repoConfig.fullName()

    fun serviceOrNull(): CmsService? = cmsService

    fun createAccessToken(authService: CmsAuthService, session: CmsAuthSession): String {
        return authService.createInstallationAccessToken(session.id, repoConfig)
    }

    fun ensureReady(authService: CmsAuthService, session: CmsAuthSession): Boolean {
        synchronized(lock) {
            cmsService?.let { return true }

            val attached = authService.attachInstallationForRepo(session.id, repoConfig) ?: return false
            val token = authService.createInstallationAccessToken(attached.id, repoConfig)
            checkoutOrUpdate(token)
            initializeWorkspace(generate = true)
            return true
        }
    }

    fun completeInstallation(
        authService: CmsAuthService,
        session: CmsAuthSession,
        state: String,
        installationId: Long
    ) {
        synchronized(lock) {
            val updatedSession = authService.completeInstallation(session.id, state, installationId, repoConfig)
            val token = authService.createInstallationAccessToken(updatedSession.id, repoConfig)
            checkoutOrUpdate(token)
            initializeWorkspace(generate = true)
        }
    }

    fun publicRoot(): Path {
        val currentConfig = siteConfig ?: return checkoutRoot.resolve(bootstrapConfig.theme.output).normalize()
        return checkoutRoot.resolve(currentConfig.theme.output).normalize()
    }

    fun status(authStatus: CmsAuthStatus?): CmsStatusResponse {
        val service = cmsService
        return if (service != null) {
            service.status().copy(
                ready = true,
                repository = repositoryName(),
                auth = authStatus
            )
        } else {
            CmsStatusResponse(
                enabled = true,
                basePath = CmsService.normalizeBasePath(bootstrapConfig.cms.basePath),
                ready = false,
                repository = repositoryName(),
                items = 0,
                dirty = 0,
                git = CmsGitStatus(
                    available = Files.exists(checkoutRoot.resolve(".git")),
                    enabled = bootstrapConfig.cms.git.enabled,
                    repoRoot = checkoutRoot.toString(),
                    branch = repoConfig.branch,
                    remote = bootstrapConfig.cms.git.remote,
                    remoteUrl = remoteUrl()
                ),
                auth = authStatus
            )
        }
    }

    private fun checkoutOrUpdate(token: String) {
        if (Files.exists(checkoutRoot.resolve(".git"))) {
            updateCheckout(token)
        } else {
            cloneCheckout(token)
        }
    }

    private fun cloneCheckout(token: String) {
        checkoutRoot.parent?.let { Files.createDirectories(it) }
        val cloneCommand = Git.cloneRepository()
            .setURI(remoteUrl())
            .setDirectory(checkoutRoot.toFile())
            .setRemote(bootstrapConfig.cms.git.remote)
            .setCredentialsProvider(credentials(token))

        repoConfig.branch?.takeIf { it.isNotBlank() }?.let { branch ->
            cloneCommand.setBranch("refs/heads/$branch")
        }

        cloneCommand.call().use { }
    }

    private fun updateCheckout(token: String) {
        Git.open(checkoutRoot.toFile()).use { git ->
            repoConfig.branch?.takeIf { it.isNotBlank() }?.let { branch ->
                ensureBranch(git, branch)
            }

            git.fetch()
                .setRemote(bootstrapConfig.cms.git.remote)
                .setCredentialsProvider(credentials(token))
                .call()

            git.pull()
                .setRemote(bootstrapConfig.cms.git.remote)
                .setCredentialsProvider(credentials(token))
                .apply {
                    repoConfig.branch?.takeIf { it.isNotBlank() }?.let { branch ->
                        setRemoteBranchName(branch)
                    }
                }
                .call()
        }
    }

    private fun ensureBranch(git: Git, branch: String) {
        if (git.repository.branch == branch) {
            return
        }

        val localRefs = git.branchList().call().map { it.name }.toSet()
        val fullBranchName = "refs/heads/$branch"
        val checkout = git.checkout().setName(branch)
        if (fullBranchName in localRefs) {
            checkout.call()
        } else {
            checkout
                .setCreateBranch(true)
                .setStartPoint("${bootstrapConfig.cms.git.remote}/$branch")
                .call()
        }
    }

    private fun initializeWorkspace(generate: Boolean) {
        val loadedConfig = BlogConfig.load(checkoutRoot.toString()).copy(cms = bootstrapConfig.cms)
        val generator = SiteGenerator(checkoutRoot.toString(), loadedConfig)
        if (generate) {
            generator.generate()
        }
        val service = CmsService(
            rootPath = checkoutRoot,
            config = loadedConfig,
            generator = generator,
            databasePath = databasePath
        )
        service.bootstrap()
        siteConfig = loadedConfig
        cmsService = service
    }

    private fun remoteUrl(): String {
        return "https://github.com/${repoConfig.ownerPart()}/${repoConfig.namePart()}.git"
    }

    private fun credentials(token: String): UsernamePasswordCredentialsProvider {
        return UsernamePasswordCredentialsProvider("x-access-token", token)
    }

    companion object {
        private fun resolveCheckoutRoot(hostRoot: Path, checkoutDir: String): Path {
            val path = Paths.get(checkoutDir)
            return if (path.isAbsolute) path else hostRoot.resolve(path).normalize()
        }

        private fun resolveDatabasePath(hostRoot: Path, configuredPath: String): Path {
            val path = Paths.get(configuredPath)
            return if (path.isAbsolute) path else hostRoot.resolve(path).normalize()
        }
    }
}
