package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.rendering.GradleRenderTask
import com.forge.rendering.RenderResult
import com.forge.rendering.LayoutNode
import com.forge.rendering.RenderMetadata
import com.forge.models.ForgeConfiguration
import com.forge.utils.ComposableAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for orchestrating Compose component rendering
 * 
 * This service coordinates:
 * - Composable function discovery and analysis
 * - Gradle task execution for headless rendering
 * - Output management and caching
 * - Error handling and retry logic
 */
@Service
class ComposeRenderService(private val project: Project) {
    
    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val gradleRenderTask = GradleRenderTask(project)
    
    // Cache for rendered components
    private val renderCache = ConcurrentHashMap<String, RenderResult>()
    
    fun initialize() {
        logger.info("ComposeRenderService initialized")
    }
    
    /**
     * Render a specific @Composable @Preview function
     */
    suspend fun renderComposable(
        composableFqn: String,
        forceRefresh: Boolean = false
    ): RenderResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                if (!forceRefresh && renderCache.containsKey(composableFqn)) {
                    logger.info("Using cached render for: $composableFqn")
                    return@withContext renderCache[composableFqn]!!
                }
                
                logger.info("Rendering composable: $composableFqn")
                
                // Validate composable exists and is a @Preview function
                if (!isValidPreviewComposable(composableFqn)) {
                    return@withContext RenderResult.error("Invalid @Preview composable: $composableFqn")
                }
                
                // Get configuration
                val forgeService = project.getService(ForgeService::class.java)
                val config = forgeService.getConfiguration()
                
                // Create temporary output directory
                val outputDir = createTempOutputDir(composableFqn)
                
                try {
                    // Execute render task
                    val result = gradleRenderTask.renderComposable(
                        composableFqn = composableFqn,
                        outputDir = outputDir,
                        maxImageSize = config.maxImageSize
                    )
                    
                    if (result.success) {
                        // Cache successful result
                        renderCache[composableFqn] = result
                        logger.info("Successfully rendered composable: $composableFqn")
                    }
                    
                    result
                } finally {
                    // Cleanup temporary files after a delay
                    serviceScope.launch {
                        kotlinx.coroutines.delay(5000) // 5 second delay
                        gradleRenderTask.cleanup(outputDir)
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to render composable: $composableFqn", e)
                RenderResult.error("Render failed: ${e.message}")
            }
        }
    }
    
    /**
     * Render all @Preview composables in a file
     */
    suspend fun renderAllPreviewsInFile(filePath: String): Map<String, RenderResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("Rendering all previews in file: $filePath")
                
                val previewComposables = findPreviewComposablesInFile(filePath)
                val results = mutableMapOf<String, RenderResult>()
                
                // Render each composable
                previewComposables.forEach { composableFqn ->
                    val result = renderComposable(composableFqn)
                    results[composableFqn] = result
                }
                
                logger.info("Rendered ${results.size} preview composables from file: $filePath")
                results
            } catch (e: Exception) {
                logger.error("Failed to render previews in file: $filePath", e)
                emptyMap()
            }
        }
    }
    
    /**
     * Get cached render result
     */
    fun getCachedRender(composableFqn: String): RenderResult? {
        return renderCache[composableFqn]
    }
    
    /**
     * Clear render cache
     */
    fun clearCache() {
        renderCache.clear()
        logger.info("Render cache cleared")
    }
    
    /**
     * Clear cache for specific composable
     */
    fun clearCacheForComposable(composableFqn: String) {
        renderCache.remove(composableFqn)
        logger.info("Cache cleared for composable: $composableFqn")
    }
    
    /**
     * Validate that a composable is a valid @Preview function
     */
    private suspend fun isValidPreviewComposable(composableFqn: String): Boolean {
        return try {
            // Parse FQN to get file path and function name
            val parts = composableFqn.split(".")
            if (parts.size < 2) return false
            
            val className = parts[parts.size - 2]
            val functionName = parts[parts.size - 1]
            
            // Find the file containing this composable
            val psiFile = findPsiFileForComposable(composableFqn)
            if (psiFile == null) {
                logger.warn("Could not find file for composable: $composableFqn")
                return false
            }
            
            // Check if it's a valid @Preview composable
            ComposableAnalyzer.isPreviewComposable(psiFile, functionName)
        } catch (e: Exception) {
            logger.error("Failed to validate composable: $composableFqn", e)
            false
        }
    }
    
    /**
     * Find all @Preview composables in a file
     */
    private suspend fun findPreviewComposablesInFile(filePath: String): List<String> {
        return try {
            val psiFile = findPsiFileByPath(filePath)
            if (psiFile == null) {
                logger.warn("Could not find file: $filePath")
                return emptyList()
            }
            
            ComposableAnalyzer.findPreviewComposables(psiFile)
        } catch (e: Exception) {
            logger.error("Failed to find preview composables in file: $filePath", e)
            emptyList()
        }
    }
    
    /**
     * Find PSI file for a composable FQN
     */
    private fun findPsiFileForComposable(composableFqn: String): com.intellij.psi.PsiFile? {
        // This is a simplified implementation
        // In a real implementation, you'd use the project's file system to find the file
        return null
    }
    
    /**
     * Find PSI file by path
     */
    private fun findPsiFileByPath(filePath: String): com.intellij.psi.PsiFile? {
        // This is a simplified implementation
        // In a real implementation, you'd use the project's file system to find the file
        return null
    }
    
    /**
     * Create temporary output directory for rendering
     */
    private fun createTempOutputDir(composableFqn: String): Path {
        val tempDir = System.getProperty("java.io.tmpdir")
        val forgeTempDir = Paths.get(tempDir, "forge-render", project.name)
        val outputDir = forgeTempDir.resolve(composableFqn.replace(".", "_"))
        
        Files.createDirectories(outputDir)
        return outputDir
    }
    
    /**
     * Get render statistics
     */
    fun getRenderStatistics(): RenderStatistics {
        val totalRenders = renderCache.size
        val successfulRenders = renderCache.values.count { it.success }
        val failedRenders = totalRenders - successfulRenders
        
        return RenderStatistics(
            totalRenders = totalRenders,
            successfulRenders = successfulRenders,
            failedRenders = failedRenders,
            cacheSize = renderCache.size
        )
    }
    
    fun getServiceScope(): CoroutineScope = serviceScope
    
    fun dispose() {
        serviceScope.cancel()
        clearCache()
        logger.info("ComposeRenderService disposed")
    }
}

/**
 * Statistics about render operations
 */
data class RenderStatistics(
    val totalRenders: Int,
    val successfulRenders: Int,
    val failedRenders: Int,
    val cacheSize: Int
) {
    val successRate: Double
        get() = if (totalRenders > 0) successfulRenders.toDouble() / totalRenders else 0.0
}
