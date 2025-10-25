package com.potomushto.statik

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.generators.SiteGenerator
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
        fun run(path: String, watch: Boolean = false, port: Int = 3000) {
            val config = BlogConfig.load(path)

            // Override baseUrl for development mode
            val devBaseUrl = if (watch) "http://localhost:$port/" else null
            val generator = SiteGenerator(path, config, devBaseUrl)

            // Generate site initially
            println("Generating site...")
            if (devBaseUrl != null) {
                println("Development mode: using baseUrl=$devBaseUrl (overriding ${config.baseUrl})")
            }
            generator.generate()

            if (watch) {
                // Start HTTP server and file watcher
                startServer(path, config, port)
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
                    staticFiles("/",
                        Paths.get(rootPath, config.theme.output).toFile()) {

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
            
            println("HTTP server started at http://localhost:$port")
        }
        
        private fun watchForChanges(rootPath: String, config: BlogConfig, generator: SiteGenerator) {
            println("Watching for file changes. Press Ctrl+C to stop...")
            
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
            if (templatesDir.exists()) pathsToWatch.add(templatesDir)
            
            // Add assets directory to watch
            val assetsDir = rootDir.resolve(config.theme.assets)
            if (assetsDir.exists()) pathsToWatch.add(assetsDir)
            
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
                            // Cancel previous task if it hasn't started yet
                            regenerationFuture?.cancel(true)
                            
                            val runnable = Runnable {
                                Thread.sleep(200) // Debounce time
                                println("\nFile changes detected. Regenerating site...")
                                generator.generate()
                                println("Site regenerated. Watching for more changes...")
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
                println("Watching stopped: ${e.message}")
            } finally {
                watchService.close()
                executorService.shutdown()
            }
        }
    }
}
