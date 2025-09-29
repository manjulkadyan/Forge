package com.forge.rendering

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Generator for creating layout tree representations from Compose components
 * 
 * This class handles:
 * - Converting Compose layout nodes to serializable tree structures
 * - Extracting layout properties (bounds, modifiers, etc.)
 * - Generating JSON representations for structural comparison
 * - Image processing and optimization
 */
class LayoutTreeGenerator {
    
    private val logger = thisLogger()
    private val json = Json { prettyPrint = true }
    
    /**
     * Generate a layout tree from Compose layout node data
     */
    fun generateLayoutTree(
        layoutNodeData: ComposeLayoutNodeData,
        includeChildren: Boolean = true
    ): LayoutNode {
        return try {
            logger.debug("Generating layout tree for node: ${layoutNodeData.id}")
            
            LayoutNode(
                id = layoutNodeData.id,
                type = layoutNodeData.type,
                bounds = Bounds(
                    x = layoutNodeData.bounds.x,
                    y = layoutNodeData.bounds.y,
                    width = layoutNodeData.bounds.width,
                    height = layoutNodeData.bounds.height
                ),
                children = if (includeChildren) {
                    layoutNodeData.children.map { generateLayoutTree(it, includeChildren) }
                } else {
                    emptyList()
                },
                properties = extractProperties(layoutNodeData),
                modifiers = extractModifiers(layoutNodeData)
            )
        } catch (e: Exception) {
            logger.error("Failed to generate layout tree for node: ${layoutNodeData.id}", e)
            LayoutNode(
                id = layoutNodeData.id,
                type = "Error",
                bounds = Bounds(0f, 0f, 0f, 0f),
                properties = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    
    /**
     * Serialize layout tree to JSON
     */
    fun serializeLayoutTree(layoutTree: LayoutNode): String {
        return try {
            json.encodeToString(layoutTree)
        } catch (e: Exception) {
            logger.error("Failed to serialize layout tree", e)
            "{}"
        }
    }
    
    /**
     * Process and optimize image for comparison
     */
    fun processImage(imageBytes: ByteArray, maxSize: Int = 2048): ByteArray {
        return try {
            val originalImage = ImageIO.read(imageBytes.inputStream())
            val processedImage = optimizeImage(originalImage, maxSize)
            
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(processedImage, "PNG", outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Failed to process image", e)
            imageBytes // Return original if processing fails
        }
    }
    
    /**
     * Extract properties from a layout node
     */
    private fun extractProperties(nodeData: ComposeLayoutNodeData): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        try {
            // Basic properties
            properties["width"] = nodeData.bounds.width.toString()
            properties["height"] = nodeData.bounds.height.toString()
            properties["x"] = nodeData.bounds.x.toString()
            properties["y"] = nodeData.bounds.y.toString()
            
            // Type-specific properties
            when (nodeData.type) {
                "Text" -> {
                    nodeData.text?.let { properties["text"] = it }
                    nodeData.textStyle?.let { properties["textStyle"] = it }
                }
                "Image" -> {
                    nodeData.imageUrl?.let { properties["imageUrl"] = it }
                    nodeData.contentDescription?.let { properties["contentDescription"] = it }
                }
                "Button" -> {
                    nodeData.text?.let { properties["buttonText"] = it }
                    nodeData.onClick?.let { properties["onClick"] = it }
                }
                "Column", "Row" -> {
                    nodeData.arrangement?.let { properties["arrangement"] = it }
                    nodeData.alignment?.let { properties["alignment"] = it }
                }
            }
            
            // Common properties
            nodeData.background?.let { properties["background"] = it }
            nodeData.padding?.let { properties["padding"] = it }
            nodeData.margin?.let { properties["margin"] = it }
            nodeData.alpha?.let { properties["alpha"] = it.toString() }
            nodeData.visibility?.let { properties["visibility"] = it }
            
        } catch (e: Exception) {
            logger.warn("Error extracting properties for node: ${nodeData.id}", e)
        }
        
        return properties
    }
    
    /**
     * Extract modifiers from a layout node
     */
    private fun extractModifiers(nodeData: ComposeLayoutNodeData): List<ModifierInfo> {
        val modifiers = mutableListOf<ModifierInfo>()
        
        try {
            // Padding modifier
            nodeData.padding?.let { padding ->
                modifiers.add(ModifierInfo(
                    type = "Padding",
                    properties = mapOf("value" to padding)
                ))
            }
            
            // Margin modifier
            nodeData.margin?.let { margin ->
                modifiers.add(ModifierInfo(
                    type = "Margin",
                    properties = mapOf("value" to margin)
                ))
            }
            
            // Background modifier
            nodeData.background?.let { background ->
                modifiers.add(ModifierInfo(
                    type = "Background",
                    properties = mapOf("color" to background)
                ))
            }
            
            // Size modifier
            if (nodeData.bounds.width > 0 && nodeData.bounds.height > 0) {
                modifiers.add(ModifierInfo(
                    type = "Size",
                    properties = mapOf(
                        "width" to nodeData.bounds.width.toString(),
                        "height" to nodeData.bounds.height.toString()
                    )
                ))
            }
            
            // Alpha modifier
            nodeData.alpha?.let { alpha ->
                modifiers.add(ModifierInfo(
                    type = "Alpha",
                    properties = mapOf("value" to alpha.toString())
                ))
            }
            
            // Clickable modifier
            nodeData.onClick?.let {
                modifiers.add(ModifierInfo(
                    type = "Clickable",
                    properties = mapOf("onClick" to it)
                ))
            }
            
        } catch (e: Exception) {
            logger.warn("Error extracting modifiers for node: ${nodeData.id}", e)
        }
        
        return modifiers
    }
    
    /**
     * Optimize image for comparison
     */
    private fun optimizeImage(image: BufferedImage, maxSize: Int): BufferedImage {
        val width = image.width
        val height = image.height
        
        // Calculate scaling factor
        val scale = if (width > maxSize || height > maxSize) {
            val scaleX = maxSize.toFloat() / width
            val scaleY = maxSize.toFloat() / height
            minOf(scaleX, scaleY)
        } else {
            1.0f
        }
        
        if (scale == 1.0f) {
            return image
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        val scaledImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = scaledImage.createGraphics()
        
        try {
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
            )
            graphics.drawImage(image, 0, 0, newWidth, newHeight, null)
        } finally {
            graphics.dispose()
        }
        
        return scaledImage
    }
    
    /**
     * Generate a summary of the layout tree for debugging
     */
    fun generateLayoutSummary(layoutTree: LayoutNode): LayoutSummary {
        val nodeCount = countNodes(layoutTree)
        val depth = calculateDepth(layoutTree)
        val types = collectTypes(layoutTree)
        
        return LayoutSummary(
            nodeCount = nodeCount,
            maxDepth = depth,
            nodeTypes = types,
            hasText = types.contains("Text"),
            hasImages = types.contains("Image"),
            hasButtons = types.contains("Button")
        )
    }
    
    private fun countNodes(node: LayoutNode): Int {
        return 1 + node.children.sumOf { countNodes(it) }
    }
    
    private fun calculateDepth(node: LayoutNode): Int {
        return if (node.children.isEmpty()) {
            1
        } else {
            1 + (node.children.maxOfOrNull { calculateDepth(it) } ?: 0)
        }
    }
    
    private fun collectTypes(node: LayoutNode): Set<String> {
        val types = mutableSetOf(node.type)
        node.children.forEach { types.addAll(collectTypes(it)) }
        return types
    }
}

/**
 * Data class representing Compose layout node data from the rendering system
 */
data class ComposeLayoutNodeData(
    val id: String,
    val type: String,
    val bounds: Bounds,
    val children: List<ComposeLayoutNodeData> = emptyList(),
    val text: String? = null,
    val textStyle: String? = null,
    val imageUrl: String? = null,
    val contentDescription: String? = null,
    val background: String? = null,
    val padding: String? = null,
    val margin: String? = null,
    val alpha: Float? = null,
    val visibility: String? = null,
    val arrangement: String? = null,
    val alignment: String? = null,
    val onClick: String? = null
)

/**
 * Summary of layout tree structure
 */
data class LayoutSummary(
    val nodeCount: Int,
    val maxDepth: Int,
    val nodeTypes: Set<String>,
    val hasText: Boolean,
    val hasImages: Boolean,
    val hasButtons: Boolean
)
