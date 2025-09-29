package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.figma.FigmaDataAcquisition
import com.forge.figma.FigmaFileData
import com.forge.figma.FigmaNodeData
import com.forge.figma.FigmaImageData
import com.forge.models.ForgeConfiguration
import com.forge.models.AuthenticationMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for acquiring Figma data
 * 
 * This service provides:
 * - Unified interface for Figma data acquisition
 * - Caching and rate limiting
 * - Error handling and retry logic
 */
@Service
class FigmaDataService(private val project: Project) {
    
    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var dataAcquisition: FigmaDataAcquisition? = null
    
    // Cache for acquired data
    private val fileCache = ConcurrentHashMap<String, FigmaFileData>()
    private val nodeCache = ConcurrentHashMap<String, FigmaNodeData>()
    private val imageCache = ConcurrentHashMap<String, FigmaImageData>()
    
    fun initialize() {
        logger.info("FigmaDataService initialized")
    }
    
    /**
     * Get file data
     */
    suspend fun getFile(fileKey: String): FigmaFileData? = withContext(Dispatchers.IO) {
        // Check cache first
        fileCache[fileKey]?.let { return@withContext it }
        
        val acquisition = getDataAcquisition() ?: return@withContext null
        val result = acquisition.getFile(fileKey)
        
        // Cache successful result
        result?.let { fileCache[fileKey] = it }
        result
    }
    
    /**
     * Get node data
     */
    suspend fun getNode(fileKey: String, nodeId: String): FigmaNodeData? = withContext(Dispatchers.IO) {
        val cacheKey = "$fileKey:$nodeId"
        nodeCache[cacheKey]?.let { return@withContext it }
        
        val acquisition = getDataAcquisition() ?: return@withContext null
        val result = acquisition.getNode(fileKey, nodeId)
        
        result?.let { nodeCache[cacheKey] = it }
        result
    }
    
    /**
     * Export node as image
     */
    suspend fun exportNode(
        fileKey: String,
        nodeId: String,
        format: String = "png",
        scale: Float = 1.0f
    ): FigmaImageData? = withContext(Dispatchers.IO) {
        val cacheKey = "$fileKey:$nodeId:$format:$scale"
        imageCache[cacheKey]?.let { return@withContext it }
        
        val acquisition = getDataAcquisition() ?: return@withContext null
        val result = acquisition.exportNode(fileKey, nodeId, format, scale)
        
        result?.let { imageCache[cacheKey] = it }
        result
    }
    
    /**
     * Validate API token
     */
    suspend fun validateToken(): Boolean = withContext(Dispatchers.IO) {
        val acquisition = getDataAcquisition() ?: return@withContext false
        acquisition.validateToken()
    }
    
    // Client initialization
    private suspend fun getDataAcquisition(): FigmaDataAcquisition? {
        if (dataAcquisition == null) {
            val config = getConfiguration()
            if (config.figmaPersonalAccessToken.isNotBlank()) {
                dataAcquisition = FigmaDataAcquisition(config.figmaPersonalAccessToken)
            }
        }
        return dataAcquisition
    }
    
    private fun getConfiguration(): ForgeConfiguration {
        val forgeService = project.getService(ForgeService::class.java)
        return forgeService.getConfiguration()
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        fileCache.clear()
        nodeCache.clear()
        imageCache.clear()
        logger.info("Figma data cache cleared")
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStatistics(): Map<String, Any> {
        return mapOf(
            "filesCached" to fileCache.size,
            "nodesCached" to nodeCache.size,
            "imagesCached" to imageCache.size,
            "totalMemoryUsage" to estimateMemoryUsage()
        )
    }
    
    private fun estimateMemoryUsage(): Long {
        var totalSize = 0L
        fileCache.values.forEach { totalSize += it.name.length * 2 + it.key.length * 2 }
        nodeCache.values.forEach { totalSize += it.rawJson.length * 2 }
        imageCache.values.forEach { totalSize += it.imageBytes.size }
        return totalSize
    }
    
    fun dispose() {
        serviceScope.cancel()
        logger.info("FigmaDataService disposed")
    }
}
