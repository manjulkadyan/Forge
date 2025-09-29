package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.comparison.VisualComparisonEngine
import com.forge.comparison.StructuralComparisonEngine
import com.forge.comparison.VisualComparisonResult
import com.forge.comparison.StructuralComparisonResult
import com.forge.models.ForgeConfiguration
import com.forge.rendering.RenderResult
import com.forge.figma.FigmaImageData
import com.forge.figma.FigmaNodeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Main comparison service that orchestrates visual and structural comparison
 * 
 * This service coordinates:
 * - Visual comparison using perceptual hashing and image analysis
 * - Structural comparison using layout tree analysis
 * - Result aggregation and reporting
 * - Caching of comparison results
 * - Integration with Phase 3 (rendering) and Phase 4 (Figma data)
 */
@Service
class ComparisonService(private val project: Project) {

    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val visualEngine = VisualComparisonEngine()
    private val structuralEngine = StructuralComparisonEngine()
    
    // Cache for comparison results
    private val comparisonCache = ConcurrentHashMap<String, ComparisonResult>()
    
    // Cache for Figma data
    private val figmaDataCache = ConcurrentHashMap<String, CachedFigmaData>()

    fun initialize() {
        logger.info("ComparisonService initialized")
    }

    /**
     * Compare Compose render with Figma design
     * 
     * @param renderResult Result from Phase 3 (Compose rendering)
     * @param figmaImageData Image data from Phase 4 (Figma data acquisition)
     * @param figmaNodeData Node data from Phase 4 (Figma data acquisition)
     * @param forceComparison If true, bypasses cache and forces new comparison
     * @return Complete comparison result
     */
    suspend fun compareWithFigma(
        renderResult: RenderResult,
        figmaImageData: FigmaImageData,
        figmaNodeData: FigmaNodeData,
        forceComparison: Boolean = false
    ): ComparisonResult = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(renderResult, figmaImageData, figmaNodeData)
            
            // Check cache first
            if (!forceComparison && comparisonCache.containsKey(cacheKey)) {
                logger.info("Returning cached comparison result for key: $cacheKey")
                return@withContext comparisonCache[cacheKey]!!
            }

            logger.info("Starting comparison with Figma design")

            // Get configuration for thresholds
            val forgeService = project.getService(ForgeService::class.java)
            val config = forgeService.getConfiguration()

            // Run visual and structural comparisons in parallel
            val visualComparison = async {
                visualEngine.compareImages(
                    composeImageBytes = renderResult.imageBytes ?: byteArrayOf(),
                    figmaImageBytes = figmaImageData.imageBytes,
                    threshold = config.visualThreshold
                )
            }

            val structuralComparison = async {
                structuralEngine.compareStructures(
                    composeLayoutTree = renderResult.layoutTree ?: createEmptyLayoutTree(),
                    figmaNodeJson = figmaNodeData.rawJson,
                    threshold = config.structuralThreshold
                )
            }

            // Wait for both comparisons to complete
            val visualResult = visualComparison.await()
            val structuralResult = structuralComparison.await()

            // Create comprehensive comparison result
            val comparisonResult = ComparisonResult(
                overallPassed = visualResult.passed && structuralResult.passed,
                visualComparison = visualResult,
                structuralComparison = structuralResult,
                visualSimilarity = visualResult.similarity,
                structuralSimilarity = structuralResult.similarity,
                overallSimilarity = calculateOverallSimilarity(visualResult.similarity, structuralResult.similarity),
                timestamp = System.currentTimeMillis(),
                renderResult = renderResult,
                figmaImageData = figmaImageData,
                figmaNodeData = figmaNodeData,
                configuration = config
            )

            // Cache the result
            comparisonCache[cacheKey] = comparisonResult

            logger.info("Comparison completed: overall=${comparisonResult.overallPassed}, " +
                       "visual=${visualResult.similarity}, structural=${structuralResult.similarity}")

            comparisonResult

        } catch (e: Exception) {
            logger.error("Error during comparison", e)
            ComparisonResult(
                overallPassed = false,
                visualComparison = VisualComparisonResult(0.0, false, error = "Visual comparison failed: ${e.message}"),
                structuralComparison = StructuralComparisonResult(0.0, false, error = "Structural comparison failed: ${e.message}"),
                visualSimilarity = 0.0,
                structuralSimilarity = 0.0,
                overallSimilarity = 0.0,
                timestamp = System.currentTimeMillis(),
                error = "Comparison failed: ${e.message}"
            )
        }
    }

    /**
     * Compare using cached Figma data
     */
    suspend fun compareWithCachedFigma(
        renderResult: RenderResult,
        figmaFileKey: String,
        nodeId: String,
        forceComparison: Boolean = false
    ): ComparisonResult? = withContext(Dispatchers.IO) {
        try {
            val figmaDataService = project.getService(FigmaDataService::class.java)
            
            // Get Figma data
            val figmaImageData = figmaDataService.exportNode(figmaFileKey, nodeId)
            val figmaNodeData = figmaDataService.getNode(figmaFileKey, nodeId)
            
            if (figmaImageData == null || figmaNodeData == null) {
                logger.warn("Failed to get Figma data for file: $figmaFileKey, node: $nodeId")
                return@withContext null
            }
            
            return@withContext compareWithFigma(renderResult, figmaImageData, figmaNodeData, forceComparison)
            
        } catch (e: Exception) {
            logger.error("Error comparing with cached Figma data", e)
            return@withContext null
        }
    }

    /**
     * Get comparison history for a specific Composable
     */
    fun getComparisonHistory(composableFqn: String): List<ComparisonResult> {
        return comparisonCache.values
            .sortedByDescending { it.timestamp }
    }

    /**
     * Clear comparison cache
     */
    fun clearCache() {
        comparisonCache.clear()
        figmaDataCache.clear()
        logger.info("Comparison cache cleared")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStatistics(): CacheStatistics {
        return CacheStatistics(
            comparisonCacheSize = comparisonCache.size,
            figmaDataCacheSize = figmaDataCache.size,
            totalMemoryUsage = estimateMemoryUsage()
        )
    }

    /**
     * Generate cache key for comparison
     */
    private fun generateCacheKey(
        renderResult: RenderResult,
        figmaImageData: FigmaImageData,
        figmaNodeData: FigmaNodeData
    ): String {
        val renderHash = renderResult.imageBytes?.contentHashCode() ?: 0
        val figmaImageHash = figmaImageData.imageBytes.contentHashCode()
        val figmaNodeHash = figmaNodeData.rawJson.hashCode()
        
        return "${renderHash}_${figmaImageHash}_${figmaNodeHash}"
    }

    /**
     * Calculate overall similarity from visual and structural similarities
     */
    private fun calculateOverallSimilarity(visualSimilarity: Double, structuralSimilarity: Double): Double {
        // Weighted average: 60% visual, 40% structural
        return (visualSimilarity * 0.6) + (structuralSimilarity * 0.4)
    }

    /**
     * Create empty layout tree for fallback
     */
    private fun createEmptyLayoutTree(): com.forge.rendering.LayoutNode {
        return com.forge.rendering.LayoutNode(
            id = "empty",
            type = "Empty",
            bounds = com.forge.rendering.Bounds(0f, 0f, 0f, 0f),
            properties = emptyMap(),
            children = emptyList()
        )
    }

    /**
     * Estimate memory usage of caches
     */
    private fun estimateMemoryUsage(): Long {
        var totalSize = 0L
        
        // Estimate comparison cache size
        comparisonCache.values.forEach { result ->
            totalSize += result.renderResult?.imageBytes?.size ?: 0
            totalSize += result.figmaImageData?.imageBytes?.size ?: 0
            totalSize += result.figmaNodeData?.rawJson?.length ?: 0 * 2 // Rough estimate for string
        }
        
        // Estimate Figma data cache size
        figmaDataCache.values.forEach { cachedData ->
            totalSize += cachedData.imageBytes.size
            totalSize += cachedData.nodeJson.length * 2
        }
        
        return totalSize
    }

    fun dispose() {
        serviceScope.cancel()
        logger.info("ComparisonService disposed")
    }
}

/**
 * Complete comparison result
 */
data class ComparisonResult(
    val overallPassed: Boolean,
    val visualComparison: VisualComparisonResult,
    val structuralComparison: StructuralComparisonResult,
    val visualSimilarity: Double,
    val structuralSimilarity: Double,
    val overallSimilarity: Double,
    val timestamp: Long,
    val renderResult: RenderResult? = null,
    val figmaImageData: FigmaImageData? = null,
    val figmaNodeData: FigmaNodeData? = null,
    val configuration: ForgeConfiguration? = null,
    val error: String? = null
) {
    fun getDetailedReport(): String {
        return buildString {
            appendLine("=== The Forge Comparison Report ===")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(timestamp))}")
            appendLine("Overall Result: ${if (overallPassed) "✅ PASSED" else "❌ FAILED"}")
            appendLine("Overall Similarity: ${(overallSimilarity * 100).toInt()}%")
            appendLine()
            
            appendLine("--- Visual Comparison ---")
            appendLine(visualComparison.getDetailedReport())
            appendLine()
            
            appendLine("--- Structural Comparison ---")
            appendLine(structuralComparison.getDetailedReport())
            appendLine()
            
            if (error != null) {
                appendLine("--- Error ---")
                appendLine(error)
            }
        }
    }
    
    fun getSummary(): String {
        return "${if (overallPassed) "✅" else "❌"} " +
               "Visual: ${(visualSimilarity * 100).toInt()}% | " +
               "Structural: ${(structuralSimilarity * 100).toInt()}% | " +
               "Overall: ${(overallSimilarity * 100).toInt()}%"
    }
}

/**
 * Cached Figma data
 */
data class CachedFigmaData(
    val imageBytes: ByteArray,
    val nodeJson: String,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedFigmaData

        if (!imageBytes.contentEquals(other.imageBytes)) return false
        if (nodeJson != other.nodeJson) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + nodeJson.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Cache statistics
 */
data class CacheStatistics(
    val comparisonCacheSize: Int,
    val figmaDataCacheSize: Int,
    val totalMemoryUsage: Long
) {
    fun getMemoryUsageMB(): Double = totalMemoryUsage / (1024.0 * 1024.0)
}