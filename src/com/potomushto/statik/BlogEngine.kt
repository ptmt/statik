package com.potomushto.statik

import com.potomushto.statik.cms.CmsService
import com.potomushto.statik.cms.CmsAuthService
import com.potomushto.statik.cms.CmsPermissionDeniedException
import com.potomushto.statik.cms.CmsWorkspaceManager
import com.potomushto.statik.cms.installCmsRoutes
import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.SiteGenerator
import com.potomushto.statik.logging.LoggerFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.*
import java.util.concurrent.Executors
import kotlin.io.path.exists

class BlogEngine {
    companion object {
        private val logger = LoggerFactory.getLogger(BlogEngine::class.java)

        fun run(path: String, watch: Boolean = false, portOverride: Int? = null, cmsOverride: Boolean = false) {
            val hostRoot = Paths.get(path)
            val config = BlogConfig.load(path)
            val cmsEnabled = cmsOverride || config.cms.enabled
            val managedRepoEnabled = cmsEnabled && config.cms.repo.enabled
            validateCmsConfiguration(config, cmsEnabled)
            val shouldServe = watch || cmsEnabled
            val resolvedPort = portOverride ?: config.devServer.port

            // Override baseUrl for development mode
            val devBaseUrl = if (watch && !managedRepoEnabled) "http://localhost:$resolvedPort/" else null
            val generator = if (managedRepoEnabled) {
                null
            } else {
                SiteGenerator(path, config, devBaseUrl, enableLiveReload = watch, isDevelopment = watch).also {
                    logger.info("Generating site...")
                    if (devBaseUrl != null) {
                        logger.info("Development mode: using baseUrl=$devBaseUrl (overriding ${config.baseUrl})")
                    }
                    it.generate()
                }
            }

            val cmsService = if (cmsEnabled && !managedRepoEnabled) {
                CmsService(Paths.get(path), config, requireNotNull(generator)).also {
                    it.bootstrap()
                    logger.info("CMS enabled at ${config.cms.basePath}")
                }
            } else {
                null
            }
            val workspaceManager = if (cmsEnabled && managedRepoEnabled) {
                CmsWorkspaceManager(hostRoot, config).also {
                    logger.info("CMS managed checkout enabled for ${it.repositoryName()}")
                }
            } else {
                null
            }
            val authService = if (cmsEnabled && config.cms.auth.enabled) {
                CmsAuthService(config.cms.auth, hostRoot = hostRoot).also {
                    logger.info("CMS GitHub OAuth enabled for ${config.cms.auth.allowedUser}")
                }
            } else {
                null
            }

            val liveReloadState = if (watch && !managedRepoEnabled) LiveReloadState() else null

            if (watch && generator != null) {
                Thread {
                    watchForChanges(path, config, generator, liveReloadState!!, cmsService)
                }.apply {
                    isDaemon = true
                    start()
                }
            }

            if (shouldServe) {
                startServer(hostRoot, config, resolvedPort, liveReloadState, cmsService, workspaceManager, authService)
            }
        }

        private fun startServer(
            rootPath: Path,
            config: BlogConfig,
            port: Int,
            liveReloadState: LiveReloadState?,
            cmsService: CmsService?,
            workspaceManager: CmsWorkspaceManager?,
            authService: CmsAuthService?
        ) {
            val json = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

            val server = embeddedServer(Netty, port = port) {
                install(StatusPages) {
                    exception<CmsPermissionDeniedException> { call, cause ->
                        call.respondText(
                            text = cause.message ?: "permissions denied",
                            status = HttpStatusCode.Forbidden
                        )
                    }
                    exception<IllegalArgumentException> { call, cause ->
                        call.respondText(
                            text = "400: ${cause.message}",
                            status = HttpStatusCode.BadRequest
                        )
                    }
                    exception<Throwable> { call, cause ->
                        call.respondText(
                            text = "500: ${cause.message}",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                    status(HttpStatusCode.NotFound) { call, _ ->
                        call.respondText(text = "404: Page Not Found", status = HttpStatusCode.NotFound)
                    }
                }

                routing {
                    if (liveReloadState != null) {
                        get("/__statik__/reload") {
                            call.respondText(
                                text = json.encodeToString(
                                    ReloadResponse.serializer(),
                                    liveReloadState.getReloadResponse()
                                ),
                                contentType = ContentType.Application.Json
                            )
                        }

                        get("/__statik__/livereload.js") {
                            call.respondText(
                                text = LIVE_RELOAD_SCRIPT,
                                contentType = ContentType.Text.JavaScript
                            )
                        }
                    }

                    if (cmsService != null) {
                        installCmsRoutes(
                            siteNameProvider = { workspaceManager?.siteName() ?: config.siteName },
                            basePath = CmsService.normalizeBasePath(config.cms.basePath),
                            cmsServiceProvider = { workspaceManager?.serviceOrNull() ?: cmsService },
                            authService = authService,
                            json = json,
                            workspaceManager = workspaceManager
                        )
                    } else if (workspaceManager != null) {
                        installCmsRoutes(
                            siteNameProvider = { workspaceManager.siteName() },
                            basePath = CmsService.normalizeBasePath(config.cms.basePath),
                            cmsServiceProvider = { workspaceManager.serviceOrNull() },
                            authService = authService,
                            json = json,
                            workspaceManager = workspaceManager
                        )
                    }

                    // Posts listing page - handle both /posts and /posts/
                    get("/posts") {
                        call.respondRedirect("/posts/", permanent = false)
                    }

                    get("/") {
                        serveGeneratedSite(call, resolvePublicRoot(rootPath, config, workspaceManager), "")
                    }

                    get("{path...}") {
                        val requestedPath = call.parameters.getAll("path")
                            ?.joinToString("/")
                            .orEmpty()
                        serveGeneratedSite(
                            call,
                            resolvePublicRoot(rootPath, config, workspaceManager),
                            requestedPath
                        )
                    }
                }
            }

            logger.info("HTTP server started at http://localhost:$port")
            server.start(wait = true)
        }

        private suspend fun serveGeneratedSite(call: ApplicationCall, publicRoot: Path, requestedPath: String) {
            val resolvedFile = resolvePublicFile(publicRoot, requestedPath)
            if (resolvedFile != null) {
                call.respondFile(resolvedFile.toFile())
                return
            }
            call.respondText(text = "404: Page Not Found", status = HttpStatusCode.NotFound)
        }

        private fun resolvePublicRoot(rootPath: Path, config: BlogConfig, workspaceManager: CmsWorkspaceManager?): Path {
            return workspaceManager?.publicRoot() ?: rootPath.resolve(config.theme.output).normalize()
        }

        private fun resolvePublicFile(publicRoot: Path, requestedPath: String): Path? {
            val normalizedRoot = publicRoot.toAbsolutePath().normalize()
            if (!Files.exists(normalizedRoot)) {
                return null
            }

            val relativePath = requestedPath.trim().removePrefix("/").removeSuffix("/")
            val candidates = if (relativePath.isBlank()) {
                listOf(normalizedRoot.resolve("index.html"))
            } else {
                buildList {
                    val direct = normalizedRoot.resolve(relativePath).normalize()
                    add(direct)
                    if (!relativePath.substringAfterLast('/', "").contains('.')) {
                        add(direct.resolve("index.html").normalize())
                    }
                }
            }

            return candidates.firstOrNull { candidate ->
                candidate.startsWith(normalizedRoot) && Files.isRegularFile(candidate)
            }
        }

        private fun validateCmsConfiguration(config: BlogConfig, cmsEnabled: Boolean) {
            if (!cmsEnabled || !config.cms.repo.enabled) {
                return
            }

            require(config.cms.auth.enabled) {
                "Managed CMS checkout requires cms.auth.enabled = true"
            }
            require(!config.cms.repo.owner.isNullOrBlank()) {
                "Managed CMS checkout requires cms.repo.owner"
            }
            require(!config.cms.repo.name.isNullOrBlank()) {
                "Managed CMS checkout requires cms.repo.name"
            }
        }

        private fun watchForChanges(
            rootPath: String,
            config: BlogConfig,
            generator: SiteGenerator,
            liveReloadState: LiveReloadState,
            cmsService: CmsService?
        ) {
            val watchService = FileSystems.getDefault().newWatchService()
            val watchKeys = mutableMapOf<WatchKey, Path>()

            fun registerDirectory(path: Path) {
                if (!Files.isDirectory(path)) return
                try {
                    val key = path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                    )
                    watchKeys[key] = path
                    logger.debug { "Registered watch for: $path" }
                } catch (e: Exception) {
                    logger.warn("Failed to register watch for $path: ${e.message}")
                }
            }

            fun registerDirectoryRecursively(path: Path) {
                if (!Files.exists(path)) return
                Files.walk(path)
                    .filter { Files.isDirectory(it) }
                    .forEach { registerDirectory(it) }
            }

            val rootDir = Paths.get(rootPath)

            // Register root directory
            registerDirectory(rootDir)

            // Register posts directory and subdirectories
            val postsDir = rootDir.resolve(config.paths.posts)
            if (postsDir.exists()) {
                registerDirectoryRecursively(postsDir)
            }

            // Register pages directories and subdirectories
            config.paths.pages.forEach { pagesPath ->
                val pagesDir = rootDir.resolve(pagesPath)
                if (pagesDir.exists()) {
                    registerDirectoryRecursively(pagesDir)
                }
            }

            // Register templates directory and subdirectories
            val templatesDir = rootDir.resolve(config.theme.templates)
            if (templatesDir.exists()) {
                registerDirectoryRecursively(templatesDir)
            }

            // Register assets directories and subdirectories
            config.theme.assets.forEach { assetPath ->
                val assetsDir = rootDir.resolve(assetPath)
                if (assetsDir.exists()) {
                    registerDirectoryRecursively(assetsDir)
                }
            }

            // Print what's being watched
            logger.info("Watching for file changes in:")
            logger.info("  • Posts: ${config.paths.posts}/ (${countSubdirectories(postsDir)} subdirectories)")
            if (config.paths.pages.size == 1) {
                val pagesDir = rootDir.resolve(config.paths.pages[0])
                logger.info("  • Pages: ${config.paths.pages[0]}/ (${countSubdirectories(pagesDir)} subdirectories)")
            } else {
                config.paths.pages.forEach { pagesPath ->
                    val pagesDir = rootDir.resolve(pagesPath)
                    logger.info("  • Pages: $pagesPath/ (${countSubdirectories(pagesDir)} subdirectories)")
                }
            }
            logger.info("  • Templates: ${config.theme.templates}/ (${countSubdirectories(templatesDir)} subdirectories)")
            if (config.theme.assets.size == 1) {
                val assetsDir = rootDir.resolve(config.theme.assets[0])
                logger.info("  • Assets: ${config.theme.assets[0]}/ (${countSubdirectories(assetsDir)} subdirectories)")
            } else {
                config.theme.assets.forEach { assetPath ->
                    val assetsDir = rootDir.resolve(assetPath)
                    logger.info("  • Assets: $assetPath/ (${countSubdirectories(assetsDir)} subdirectories)")
                }
            }
            logger.info("  • Config: config.json")
            logger.info("Total: ${watchKeys.size} directories registered for watching")
            logger.info("Press Ctrl+C to stop...")

            // Use executor service for debouncing
            val executorService = Executors.newSingleThreadExecutor()
            var regenerationFuture: java.util.concurrent.Future<*>? = null

            // Start watching loop
            try {
                while (true) {
                    val key = watchService.take()
                    val watchedPath = watchKeys[key]

                    if (watchedPath != null) {
                        val events = key.pollEvents()
                        if (events.isNotEmpty()) {
                            val changedFiles = mutableListOf<Path>()

                            events.forEach { event ->
                                val context = event.context()
                                if (context is Path) {
                                    val fullPath = watchedPath.resolve(context)
                                    val kind = event.kind()

                                    // Log the specific change
                                    when (kind) {
                                        StandardWatchEventKinds.ENTRY_CREATE -> {
                                            logger.debug { "Created: $fullPath" }
                                            // If a new directory is created, register it for watching
                                            if (Files.isDirectory(fullPath)) {
                                                logger.info("New directory detected: $fullPath - registering for watching")
                                                registerDirectoryRecursively(fullPath)
                                            } else {
                                                changedFiles.add(fullPath)
                                            }
                                        }
                                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                                            logger.debug { "Modified: $fullPath" }
                                            if (Files.isRegularFile(fullPath)) {
                                                changedFiles.add(fullPath)
                                            }
                                        }
                                        StandardWatchEventKinds.ENTRY_DELETE -> {
                                            logger.debug { "Deleted: $fullPath" }
                                            changedFiles.add(fullPath)
                                        }
                                    }
                                }
                            }

                            if (changedFiles.isEmpty()) {
                                key.reset()
                                continue
                            }

                            // Cancel previous task if it hasn't started yet
                            regenerationFuture?.cancel(true)

                            val runnable = Runnable {
                                Thread.sleep(200) // Debounce time
                                logger.info("Changes detected:")
                                changedFiles.forEach { file ->
                                    logger.info("  → ${rootDir.relativize(file)}")
                                }
                                logger.info("Regenerating site...")
                                generator.regenerate(changedFiles)
                                cmsService?.refreshIndex()
                                liveReloadState.markRebuilt()
                                logger.info("Site regenerated successfully. Watching for more changes...")
                            }
                            regenerationFuture = executorService.submit(runnable)
                        }
                    }

                    // Reset the key to receive further events
                    val valid = key.reset()
                    if (!valid) {
                        logger.warn("Watch key no longer valid for: $watchedPath")
                        watchKeys.remove(key)

                        if (watchKeys.isEmpty()) {
                            logger.warn("No directories left to watch; stopping watcher loop")
                            break
                        }

                        continue
                    }
                }
            } catch (e: Exception) {
                logger.error("Watching stopped", e)
            } finally {
                watchService.close()
                executorService.shutdown()
            }
        }

        private fun countSubdirectories(path: Path): Int {
            if (!Files.exists(path)) return 0
            return Files.walk(path)
                .filter { Files.isDirectory(it) && it != path }
                .count()
                .toInt()
        }

        private const val LIVE_RELOAD_SCRIPT = """
(function() {
    console.log('[Statik Live Reload] Initializing...');

    let lastTimestamp = null;
    const pollInterval = 1000; // Poll every second

    function checkForUpdates() {
        fetch('/__statik__/reload')
            .then(response => response.json())
            .then(data => {
                if (lastTimestamp === null) {
                    // First poll, just store the timestamp
                    lastTimestamp = data.timestamp;
                    console.log('[Statik Live Reload] Connected. Watching for changes...');
                } else if (data.timestamp > lastTimestamp) {
                    // Site was rebuilt, reload the page
                    console.log('[Statik Live Reload] Changes detected, reloading page...');
                    window.location.reload();
                }
            })
            .catch(error => {
                console.error('[Statik Live Reload] Error checking for updates:', error);
            });
    }

    // Start polling
    setInterval(checkForUpdates, pollInterval);
    checkForUpdates(); // Initial check
})();
"""
    }
}

/**
 * Response model for the /posts endpoint
 */
@Serializable
data class PostsResponse(
    val posts: List<PostSummary>,
    val total: Int
)

/**
 * Summary of a blog post for the API
 */
@Serializable
data class PostSummary(
    val id: String,
    val title: String,
    val date: String,
    val path: String,
    val tags: List<String>,
    val metadata: Map<String, String>
)
