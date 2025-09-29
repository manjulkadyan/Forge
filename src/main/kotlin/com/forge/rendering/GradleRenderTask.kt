package com.forge.rendering

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Gradle task bridge for headless Compose rendering
 * 
 * This class orchestrates the execution of Gradle tasks that perform:
 * - Headless rendering of @Composable @Preview functions
 * - Generation of high-fidelity bitmaps
 * - Creation of layout tree JSON representations
 * - Output management and cleanup
 */
class GradleRenderTask(private val project: Project) {
    
    private val logger = thisLogger()
    private val json = Json { prettyPrint = true }
    
    /**
     * Render a Compose component and generate both visual and structural data
     */
    suspend fun renderComposable(
        composableFqn: String,
        outputDir: Path,
        maxImageSize: Int = 2048
    ): RenderResult {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("Starting render task for composable: $composableFqn")
                
                // Create output directory
                Files.createDirectories(outputDir)
                
                // Generate unique task name to avoid conflicts
                val taskName = "renderComposable_${System.currentTimeMillis()}"
                
                // Execute Gradle task
                val result = executeGradleRenderTask(
                    taskName = taskName,
                    composableFqn = composableFqn,
                    outputDir = outputDir.toString(),
                    maxImageSize = maxImageSize
                )
                
                if (result.success) {
                    val renderData = loadRenderOutput(outputDir, composableFqn)
                    logger.info("Render task completed successfully for: $composableFqn")
                    renderData
                } else {
                    logger.error("Render task failed for: $composableFqn - ${result.error}")
                    RenderResult.error("Gradle task failed: ${result.error}")
                }
            } catch (e: Exception) {
                logger.error("Failed to render composable: $composableFqn", e)
                RenderResult.error("Render failed: ${e.message}")
            }
        }
    }
    
    /**
     * Execute the Gradle render task
     */
    private suspend fun executeGradleRenderTask(
        taskName: String,
        composableFqn: String,
        outputDir: String,
        maxImageSize: Int
    ): GradleTaskResult {
        return withContext(Dispatchers.IO) {
            try {
                val gradleCommand = buildGradleCommand(taskName, composableFqn, outputDir, maxImageSize)
                logger.info("Executing Gradle command: $gradleCommand")
                
                val process = ProcessBuilder(gradleCommand)
                    .directory(project.baseDir?.toNioPath()?.toFile())
                    .redirectErrorStream(true)
                    .start()
                
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    GradleTaskResult.success(output)
                } else {
                    GradleTaskResult.error("Gradle task failed with exit code $exitCode: $output")
                }
            } catch (e: Exception) {
                GradleTaskResult.error("Failed to execute Gradle task: ${e.message}")
            }
        }
    }
    
    /**
     * Build the Gradle command for rendering
     */
    private fun buildGradleCommand(
        taskName: String,
        composableFqn: String,
        outputDir: String,
        maxImageSize: Int
    ): List<String> {
        return listOf(
            "./gradlew",
            ":$taskName",
            "-PcomposableFqn=$composableFqn",
            "-PoutputDir=$outputDir",
            "-PmaxImageSize=$maxImageSize",
            "--quiet"
        )
    }
    
    /**
     * Load the rendered output from the output directory
     */
    private fun loadRenderOutput(outputDir: Path, composableFqn: String): RenderResult {
        return try {
            val imageFile = outputDir.resolve("${composableFqn.replace(".", "_")}.png")
            val layoutFile = outputDir.resolve("${composableFqn.replace(".", "_")}_layout.json")
            
            if (!Files.exists(imageFile) || !Files.exists(layoutFile)) {
                return RenderResult.error("Output files not found in $outputDir")
            }
            
            val imageBytes = Files.readAllBytes(imageFile)
            val layoutJson = Files.readString(layoutFile)
            
            // Parse layout tree
            val layoutTree = json.decodeFromString<LayoutNode>(layoutJson)
            
            RenderResult.success(
                imageBytes = imageBytes,
                layoutTree = layoutTree,
                metadata = RenderMetadata(
                    composableFqn = composableFqn,
                    imageSize = imageBytes.size,
                    renderTime = System.currentTimeMillis(),
                    outputDir = outputDir.toString()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to load render output", e)
            RenderResult.error("Failed to load output: ${e.message}")
        }
    }
    
    /**
     * Clean up temporary files
     */
    fun cleanup(outputDir: Path) {
        try {
            if (Files.exists(outputDir)) {
                FileUtil.deleteRecursively(outputDir)
                logger.info("Cleaned up render output directory: $outputDir")
            }
        } catch (e: Exception) {
            logger.warn("Failed to cleanup output directory: $outputDir", e)
        }
    }
}

/**
 * Result of a Gradle task execution
 */
data class GradleTaskResult(
    val success: Boolean,
    val output: String? = null,
    val error: String? = null
) {
    companion object {
        fun success(output: String) = GradleTaskResult(true, output)
        fun error(error: String) = GradleTaskResult(false, error = error)
    }
}

/**
 * Result of a Compose render operation
 */
data class RenderResult(
    val success: Boolean,
    val imageBytes: ByteArray? = null,
    val layoutTree: LayoutNode? = null,
    val metadata: RenderMetadata? = null,
    val error: String? = null
) {
    companion object {
        fun success(
            imageBytes: ByteArray,
            layoutTree: LayoutNode,
            metadata: RenderMetadata
        ) = RenderResult(true, imageBytes, layoutTree, metadata)
        
        fun error(error: String) = RenderResult(false, error = error)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as RenderResult
        
        if (success != other.success) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (layoutTree != other.layoutTree) return false
        if (metadata != other.metadata) return false
        if (error != other.error) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (layoutTree?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

/**
 * Metadata about a render operation
 */
@Serializable
data class RenderMetadata(
    val composableFqn: String,
    val imageSize: Int,
    val renderTime: Long,
    val outputDir: String
)

/**
 * Layout node representation for structural comparison
 */
@Serializable
data class LayoutNode(
    val id: String,
    val type: String,
    val bounds: Bounds,
    val children: List<LayoutNode> = emptyList(),
    val properties: Map<String, String> = emptyMap(),
    val modifiers: List<ModifierInfo> = emptyList()
)

/**
 * Bounds information for a layout node
 */
@Serializable
data class Bounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Modifier information for a layout node
 */
@Serializable
data class ModifierInfo(
    val type: String,
    val properties: Map<String, String> = emptyMap()
)
