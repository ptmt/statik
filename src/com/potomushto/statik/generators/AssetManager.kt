package com.potomushto.statik.generators

import com.potomushto.statik.config.BlogConfig
import com.potomushto.statik.logging.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Responsible for managing and copying static assets
 */
class AssetManager(
    private val rootPath: String,
    private val config: BlogConfig,
    private val fileWalker: FileWalker
) {
    private val logger = LoggerFactory.getLogger(AssetManager::class.java)
    private val outputPath = Paths.get(rootPath, config.theme.output)
    private val assetDirectories = config.theme.assets.map { it to Paths.get(rootPath, it) }

    /**
     * Copy all static assets from configured asset directories
     */
    fun copyAllAssets() {
        logger.debug { "Copying all assets" }
        config.theme.assets.forEach { assetPath ->
            copyAssetsFromDirectory(assetPath)
        }
    }

    /**
     * Copy a single asset file to the output directory
     */
    fun copySingleAsset(assetFile: Path) {
        // Determine which asset directory this file belongs to
        val assetEntry = assetDirectories
            .firstOrNull { (_, directory) -> assetFile.startsWith(directory) }

        if (assetEntry == null) {
            logger.warn { "Asset file $assetFile does not belong to any configured asset directory" }
            return
        }

        val (assetPath, assetDir) = assetEntry
        val relativePath = assetDir.relativize(assetFile)
        val destination = resolveDestination(assetPath, relativePath)

        Files.createDirectories(destination.parent)
        Files.copy(assetFile, destination, StandardCopyOption.REPLACE_EXISTING)
        logger.debug { "Copied asset: $relativePath" }
    }

    /**
     * Copy all assets from a specific directory
     */
    private fun copyAssetsFromDirectory(assetPath: String) {
        val assetsRoot = Paths.get(rootPath, assetPath)
        if (!Files.exists(assetsRoot)) {
            logger.debug { "Asset directory does not exist: $assetPath" }
            return
        }

        fileWalker.walkStaticFiles(assetPath)
            .forEach { source ->
                val relativePath = assetsRoot.relativize(source)
                val destination = resolveDestination(assetPath, relativePath)
                Files.createDirectories(destination.parent)
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    private fun resolveDestination(assetPath: String, relativePath: Path): Path {
        val destinationRoot = if (shouldFlatten(assetPath)) {
            outputPath
        } else {
            outputPath.resolve(Paths.get(assetPath))
        }

        return destinationRoot.resolve(relativePath)
    }

    private fun shouldFlatten(assetPath: String): Boolean {
        val lastSegment = Paths.get(assetPath).fileName?.toString() ?: assetPath
        return lastSegment in setOf("public", "static")
    }
}
