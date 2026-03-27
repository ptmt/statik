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
        val target = resolveTarget(assetFile) ?: run {
            logger.warn { "Asset file $assetFile does not belong to any configured asset directory" }
            return
        }

        Files.createDirectories(target.destination.parent)
        Files.copy(assetFile, target.destination, StandardCopyOption.REPLACE_EXISTING)
        logger.debug { "Copied asset: ${target.relativePath}" }
    }

    fun deleteSingleAsset(sourcePath: String) {
        val target = resolveTarget(sourcePath) ?: run {
            logger.warn { "Asset path $sourcePath does not belong to any configured asset directory" }
            return
        }

        Files.deleteIfExists(target.destination)
        pruneEmptyDirectories(target.destination.parent, target.destinationRoot)
        logger.debug { "Deleted asset: ${target.relativePath}" }
    }

    fun publicPathForSourcePath(sourcePath: String): String? {
        val target = resolveTarget(sourcePath) ?: return null
        val relative = outputPath.relativize(target.destination).toString().replace('\\', '/')
        return "/" + relative.removePrefix("/")
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

    private fun resolveTarget(sourcePath: String): AssetTarget? {
        val normalized = sourcePath.trim().removePrefix("/").replace('\\', '/')
        val assetEntry = assetDirectories
            .firstOrNull { (assetPath, _) -> normalized == assetPath || normalized.startsWith("$assetPath/") }
            ?: return null

        val (assetPath, assetDir) = assetEntry
        val relativePath = Paths.get(normalized.removePrefix(assetPath).removePrefix("/"))
        val destinationRoot = destinationRoot(assetPath)
        return AssetTarget(
            relativePath = relativePath,
            destination = destinationRoot.resolve(relativePath),
            destinationRoot = destinationRoot
        )
    }

    private fun resolveTarget(assetFile: Path): AssetTarget? {
        val assetEntry = assetDirectories
            .firstOrNull { (_, directory) -> assetFile.startsWith(directory) }
            ?: return null

        val (assetPath, assetDir) = assetEntry
        val relativePath = assetDir.relativize(assetFile)
        val destinationRoot = destinationRoot(assetPath)
        return AssetTarget(
            relativePath = relativePath,
            destination = destinationRoot.resolve(relativePath),
            destinationRoot = destinationRoot
        )
    }

    private fun resolveDestination(assetPath: String, relativePath: Path): Path {
        return destinationRoot(assetPath).resolve(relativePath)
    }

    private fun destinationRoot(assetPath: String): Path {
        return if (shouldFlatten(assetPath)) {
            outputPath
        } else {
            outputPath.resolve(Paths.get(assetPath))
        }
    }

    private fun shouldFlatten(assetPath: String): Boolean {
        val normalized = assetPath.trimEnd('/', '\\')
        val lastSegment = Paths.get(normalized).fileName?.toString() ?: normalized
        return lastSegment == "public" || lastSegment == "static"
    }

    private fun pruneEmptyDirectories(start: Path?, stopAt: Path) {
        var current = start
        while (current != null && current.startsWith(stopAt) && current != stopAt) {
            val isEmpty = Files.list(current).use { stream -> !stream.findFirst().isPresent }
            if (!isEmpty) {
                return
            }
            Files.deleteIfExists(current)
            current = current.parent
        }
    }

    private data class AssetTarget(
        val relativePath: Path,
        val destination: Path,
        val destinationRoot: Path
    )
}
