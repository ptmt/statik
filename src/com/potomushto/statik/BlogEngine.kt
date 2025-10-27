package com.potomushto.statik

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
import io.ktor.server.http.content.*
import java.nio.file.*
import java.util.concurrent.Executors
import kotlin.io.path.exists

class BlogEngine {
    companion object {
        private val logger = LoggerFactory.getLogger(BlogEngine::class.java)

        fun run(path: String, watch: Boolean = false, portOverride: Int? = null) {
            val config = BlogConfig.load(path)
            val resolvedPort = portOverride ?: config.devServer.port

            // Override baseUrl for development mode
            val devBaseUrl = if (watch) "http://localhost:$resolvedPort/" else null
            val generator = SiteGenerator(path, config, devBaseUrl)

            // Generate site initially
            logger.info("Generating site...")
            if (devBaseUrl != null) {
                logger.info("Development mode: using baseUrl=$devBaseUrl (overriding ${config.baseUrl})")
            }
            generator.generate()

            if (watch) {
                // Start HTTP server and file watcher
                startServer(path, config, resolvedPort)
                watchForChanges(path, config, generator)
            }
        }

        private fun startServer(rootPath: String, config: BlogConfig, port: Int) {
            val server = embeddedServer(Netty, port = port) {
                install(StatusPages) {
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
                    staticFiles(
                        "/",
                        Paths.get(rootPath, config.theme.output).toFile()
                    ) {

                    }
                }
            }

            // Start server in a separate thread
            Thread {
                server.start(wait = true)
            }.apply {
                isDaemon = true
                start()
            }

            logger.info("HTTP server started at http://localhost:$port")
        }

        private fun watchForChanges(rootPath: String, config: BlogConfig, generator: SiteGenerator) {
            val watchService = FileSystems.getDefault().newWatchService()
            val pathsToWatch = mutableSetOf<Path>()

            // Add root directory to watch
            val rootDir = Paths.get(rootPath)
            pathsToWatch.add(rootDir)

            // Add posts directory to watch
            val postsDir = rootDir.resolve(config.paths.posts)
            if (postsDir.exists()) pathsToWatch.add(postsDir)

            val pagesDir = rootDir.resolve(config.paths.pages)
            if (pagesDir.exists()) pathsToWatch.add(pagesDir)

            // Add templates directory to watch
            val templatesDir = rootDir.resolve(config.theme.templates)
            if (templatesDir.exists()) {
                pathsToWatch.add(templatesDir)
                // Recursively add all subdirectories
                Files.walk(templatesDir)
                    .filter { Files.isDirectory(it) && it != templatesDir }
                    .forEach { pathsToWatch.add(it) }
            }

            // Add assets directories to watch
            config.theme.assets.forEach { assetPath ->
                val assetsDir = rootDir.resolve(assetPath)
                if (assetsDir.exists()) {
                    pathsToWatch.add(assetsDir)
                    // Recursively add all subdirectories
                    Files.walk(assetsDir)
                        .filter { Files.isDirectory(it) && it != assetsDir }
                        .forEach { pathsToWatch.add(it) }
                }
            }

            // Print what's being watched
            logger.info("Watching for file changes in:")
            logger.info("  • Posts: ${config.paths.posts}/")
            logger.info("  • Pages: ${config.paths.pages}/")
            logger.info("  • Templates: ${config.theme.templates}/")
            if (config.theme.assets.size == 1) {
                logger.info("  • Assets: ${config.theme.assets[0]}/")
            } else {
                logger.info("  • Assets: ${config.theme.assets.joinToString(", ") { "$it/" }}")
            }
            logger.info("  • Config: config.json")
            logger.info("Press Ctrl+C to stop...")

            // Register all directories for watching
            val watchKeys = pathsToWatch.associateWith { path ->
                path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                )
            }

            // Use executor service for debouncing
            val executorService = Executors.newSingleThreadExecutor()
            var regenerationFuture: java.util.concurrent.Future<*>? = null

            // Start watching loop
            try {
                while (true) {
                    val key = watchService.take()
                    val path = watchKeys.entries.find { it.value == key }?.key

                    if (path != null) {
                        val events = key.pollEvents()
                        if (events.isNotEmpty()) {
                            // Collect changed files
                            val changedFiles = events
                                .mapNotNull { event ->
                                    val context = event.context()
                                    if (context is Path) {
                                        path.resolve(context)
                                    } else null
                                }
                                .filter { Files.isRegularFile(it) || !Files.exists(it) } // Include deleted files

                            if (changedFiles.isEmpty()) {
                                key.reset()
                                continue
                            }

                            // Cancel previous task if it hasn't started yet
                            regenerationFuture?.cancel(true)

                            val runnable = Runnable {
                                Thread.sleep(200) // Debounce time
                                logger.info("File changes detected: ${changedFiles.size} file(s). Regenerating site...")
                                generator.regenerate(changedFiles)
                                logger.info("Site regenerated. Watching for more changes...")
                            }
                            regenerationFuture = executorService.submit(runnable)
                        }
                    }

                    // Reset the key to receive further events
                    val valid = key.reset()
                    if (!valid) {
                        break
                    }
                }
            } catch (e: Exception) {
                logger.error("Watching stopped", e)
            } finally {
                watchService.close()
                executorService.shutdown()
            }
        }
    }
}
