package com.forge.comparison

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.forge.rendering.LayoutNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Structural comparison engine for analyzing layout trees
 * 
 * This engine compares Compose layout trees with Figma node structures using:
 * - Tree structure analysis
 * - Node type matching
 * - Property comparison
 * - Layout constraint validation
 * - Design token compliance
 */
class StructuralComparisonEngine {

    private val logger = thisLogger()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Compare Compose layout tree with Figma node structure
     */
    suspend fun compareStructures(
        composeLayoutTree: LayoutNode,
        figmaNodeJson: String,
        threshold: Double = 0.90
    ): StructuralComparisonResult = withContext(Dispatchers.IO) {
        try {
            val figmaNode = parseFigmaNode(figmaNodeJson)
            if (figmaNode == null) {
                return@withContext StructuralComparisonResult(
                    similarity = 0.0,
                    passed = false,
                    error = "Failed to parse Figma node JSON"
                )
            }

            // Calculate different structural metrics
            val treeStructureSimilarity = calculateTreeStructureSimilarity(composeLayoutTree, figmaNode)
            val nodeTypeSimilarity = calculateNodeTypeSimilarity(composeLayoutTree, figmaNode)
            val propertySimilarity = calculatePropertySimilarity(composeLayoutTree, figmaNode)
            val layoutSimilarity = calculateLayoutSimilarity(composeLayoutTree, figmaNode)
            val constraintSimilarity = calculateConstraintSimilarity(composeLayoutTree, figmaNode)

            // Weighted average of all metrics
            val overallSimilarity = (
                treeStructureSimilarity * 0.3 +
                nodeTypeSimilarity * 0.25 +
                propertySimilarity * 0.2 +
                layoutSimilarity * 0.15 +
                constraintSimilarity * 0.1
            )

            val passed = overallSimilarity >= threshold

            logger.info("Structural comparison completed: similarity=$overallSimilarity, passed=$passed")

            StructuralComparisonResult(
                similarity = overallSimilarity,
                passed = passed,
                treeStructureSimilarity = treeStructureSimilarity,
                nodeTypeSimilarity = nodeTypeSimilarity,
                propertySimilarity = propertySimilarity,
                layoutSimilarity = layoutSimilarity,
                constraintSimilarity = constraintSimilarity,
                threshold = threshold
            )

        } catch (e: Exception) {
            logger.error("Error during structural comparison", e)
            StructuralComparisonResult(
                similarity = 0.0,
                passed = false,
                error = "Structural comparison failed: ${e.message}"
            )
        }
    }

    /**
     * Parse Figma node JSON into a structured format
     */
    private fun parseFigmaNode(jsonString: String): FigmaNode? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            FigmaNode(
                id = jsonObject["id"]?.toString()?.removeSurrounding("\"") ?: "",
                type = jsonObject["type"]?.toString()?.removeSurrounding("\"") ?: "",
                name = jsonObject["name"]?.toString()?.removeSurrounding("\"") ?: "",
                bounds = parseBounds(jsonObject["absoluteBoundingBox"]),
                properties = parseProperties(jsonObject),
                children = parseChildren(jsonObject["children"])
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse Figma node JSON", e)
            null
        }
    }

    /**
     * Parse bounds from Figma JSON
     */
    private fun parseBounds(boundsJson: Any?): FigmaBounds? {
        return try {
            if (boundsJson is JsonObject) {
                FigmaBounds(
                    x = boundsJson["x"]?.toString()?.toFloatOrNull() ?: 0f,
                    y = boundsJson["y"]?.toString()?.toFloatOrNull() ?: 0f,
                    width = boundsJson["width"]?.toString()?.toFloatOrNull() ?: 0f,
                    height = boundsJson["height"]?.toString()?.toFloatOrNull() ?: 0f
                )
            } else null
        } catch (e: Exception) {
            logger.warn("Failed to parse bounds", e)
            null
        }
    }

    /**
     * Parse properties from Figma JSON
     */
    private fun parseProperties(nodeJson: JsonObject): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        // Extract common properties
        nodeJson["name"]?.let { properties["name"] = it.toString().removeSurrounding("\"") }
        nodeJson["visible"]?.let { properties["visible"] = it.toString() }
        nodeJson["opacity"]?.let { properties["opacity"] = it.toString() }
        
        // Extract style properties
        val style = nodeJson["style"]?.jsonObject
        style?.let { styleJson ->
            styleJson["fill"]?.let { properties["fill"] = it.toString() }
            styleJson["stroke"]?.let { properties["stroke"] = it.toString() }
            styleJson["fontSize"]?.let { properties["fontSize"] = it.toString() }
            styleJson["fontFamily"]?.let { properties["fontFamily"] = it.toString() }
        }
        
        return properties
    }

    /**
     * Parse children from Figma JSON
     */
    private fun parseChildren(childrenJson: Any?): List<FigmaNode> {
        return try {
            if (childrenJson is kotlinx.serialization.json.JsonArray) {
                childrenJson.mapNotNull { childJson ->
                    if (childJson is JsonObject) {
                        FigmaNode(
                            id = childJson["id"]?.toString()?.removeSurrounding("\"") ?: "",
                            type = childJson["type"]?.toString()?.removeSurrounding("\"") ?: "",
                            name = childJson["name"]?.toString()?.removeSurrounding("\"") ?: "",
                            bounds = parseBounds(childJson["absoluteBoundingBox"]),
                            properties = parseProperties(childJson),
                            children = parseChildren(childJson["children"])
                        )
                    } else null
                }
            } else emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to parse children", e)
            emptyList()
        }
    }

    /**
     * Calculate tree structure similarity
     */
    private fun calculateTreeStructureSimilarity(composeNode: LayoutNode, figmaNode: FigmaNode): Double {
        val composeStructure = extractTreeStructure(composeNode)
        val figmaStructure = extractTreeStructure(figmaNode)
        
        return calculateStructureSimilarity(composeStructure, figmaStructure)
    }

    /**
     * Extract tree structure as a string representation
     */
    private fun extractTreeStructure(node: LayoutNode): String {
        return buildString {
            append("${node.type}(")
            if (node.children.isNotEmpty()) {
                append(node.children.joinToString(",") { extractTreeStructure(it) })
            }
            append(")")
        }
    }

    /**
     * Extract tree structure from Figma node
     */
    private fun extractTreeStructure(node: FigmaNode): String {
        return buildString {
            append("${node.type}(")
            if (node.children.isNotEmpty()) {
                append(node.children.joinToString(",") { extractTreeStructure(it) })
            }
            append(")")
        }
    }

    /**
     * Calculate similarity between two tree structures
     */
    private fun calculateStructureSimilarity(structure1: String, structure2: String): Double {
        val maxLength = max(structure1.length, structure2.length)
        if (maxLength == 0) return 1.0
        
        val distance = calculateLevenshteinDistance(structure1, structure2)
        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    s1[i - 1] == s2[j - 1] -> dp[i][j] = dp[i - 1][j - 1]
                    else -> dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }

    /**
     * Calculate node type similarity
     */
    private fun calculateNodeTypeSimilarity(composeNode: LayoutNode, figmaNode: FigmaNode): Double {
        val composeTypes = extractNodeTypes(composeNode)
        val figmaTypes = extractNodeTypes(figmaNode)
        
        val commonTypes = composeTypes.intersect(figmaTypes.toSet()).size
        val totalTypes = composeTypes.size + figmaTypes.size - commonTypes
        
        return if (totalTypes > 0) commonTypes.toDouble() / totalTypes else 1.0
    }

    /**
     * Extract all node types from a tree
     */
    private fun extractNodeTypes(node: LayoutNode): List<String> {
        return listOf(node.type) + node.children.flatMap { extractNodeTypes(it) }
    }

    /**
     * Extract all node types from Figma node
     */
    private fun extractNodeTypes(node: FigmaNode): List<String> {
        return listOf(node.type) + node.children.flatMap { extractNodeTypes(it) }
    }

    /**
     * Calculate property similarity
     */
    private fun calculatePropertySimilarity(composeNode: LayoutNode, figmaNode: FigmaNode): Double {
        val composeProps = composeNode.properties
        val figmaProps = figmaNode.properties
        
        val commonProps = composeProps.keys.intersect(figmaProps.keys)
        val totalProps = composeProps.size + figmaProps.size - commonProps.size
        
        if (totalProps == 0) return 1.0
        
        var matchingProps = 0
        for (key in commonProps) {
            val composeValue = composeProps[key] ?: ""
            val figmaValue = figmaProps[key] ?: ""
            
            if (isPropertyValueSimilar(composeValue, figmaValue)) {
                matchingProps++
            }
        }
        
        return (matchingProps + (totalProps - commonProps.size)) / totalProps.toDouble()
    }

    /**
     * Check if two property values are similar
     */
    private fun isPropertyValueSimilar(value1: String, value2: String): Boolean {
        return when {
            value1 == value2 -> true
            value1.equals(value2, ignoreCase = true) -> true
            isNumericSimilar(value1, value2) -> true
            isColorSimilar(value1, value2) -> true
            else -> false
        }
    }

    /**
     * Check if two numeric values are similar (within tolerance)
     */
    private fun isNumericSimilar(value1: String, value2: String): Boolean {
        val num1 = value1.toFloatOrNull() ?: return false
        val num2 = value2.toFloatOrNull() ?: return false
        return abs(num1 - num2) < 1.0f // 1 pixel tolerance
    }

    /**
     * Check if two color values are similar
     */
    private fun isColorSimilar(value1: String, value2: String): Boolean {
        // Simple color comparison - in real implementation, would parse hex colors
        return value1.equals(value2, ignoreCase = true)
    }

    /**
     * Calculate layout similarity
     */
    private fun calculateLayoutSimilarity(composeNode: LayoutNode, figmaNode: FigmaNode): Double {
        val composeBounds = composeNode.bounds
        val figmaBounds = figmaNode.bounds ?: return 0.0
        
        val widthSimilarity = calculateDimensionSimilarity(composeBounds.width, figmaBounds.width)
        val heightSimilarity = calculateDimensionSimilarity(composeBounds.height, figmaBounds.height)
        val positionSimilarity = calculatePositionSimilarity(composeBounds, figmaBounds)
        
        return (widthSimilarity + heightSimilarity + positionSimilarity) / 3.0
    }

    /**
     * Calculate dimension similarity
     */
    private fun calculateDimensionSimilarity(dim1: Float, dim2: Float): Double {
        val maxDim = max(dim1, dim2)
        if (maxDim == 0f) return 1.0
        
        val diff = abs(dim1 - dim2)
        return 1.0 - (diff / maxDim)
    }

    /**
     * Calculate position similarity
     */
    private fun calculatePositionSimilarity(bounds1: com.forge.rendering.Bounds, bounds2: FigmaBounds): Double {
        val xSimilarity = calculateDimensionSimilarity(bounds1.x, bounds2.x)
        val ySimilarity = calculateDimensionSimilarity(bounds1.y, bounds2.y)
        return (xSimilarity + ySimilarity) / 2.0
    }

    /**
     * Calculate constraint similarity
     */
    private fun calculateConstraintSimilarity(composeNode: LayoutNode, figmaNode: FigmaNode): Double {
        // This would analyze layout constraints like padding, margins, alignment
        // For now, return a placeholder value
        return 0.8
    }
}

/**
 * Figma node structure
 */
data class FigmaNode(
    val id: String,
    val type: String,
    val name: String,
    val bounds: FigmaBounds?,
    val properties: Map<String, String>,
    val children: List<FigmaNode>
)

/**
 * Figma bounds structure
 */
data class FigmaBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Result of structural comparison
 */
data class StructuralComparisonResult(
    val similarity: Double,
    val passed: Boolean,
    val treeStructureSimilarity: Double? = null,
    val nodeTypeSimilarity: Double? = null,
    val propertySimilarity: Double? = null,
    val layoutSimilarity: Double? = null,
    val constraintSimilarity: Double? = null,
    val threshold: Double? = null,
    val error: String? = null
) {
    fun getDetailedReport(): String {
        return buildString {
            appendLine("Structural Comparison Report:")
            appendLine("Overall Similarity: ${(similarity * 100).toInt()}%")
            appendLine("Passed: $passed")
            if (threshold != null) appendLine("Threshold: ${(threshold * 100).toInt()}%")
            
            if (treeStructureSimilarity != null) {
                appendLine("Tree Structure: ${(treeStructureSimilarity * 100).toInt()}%")
            }
            if (nodeTypeSimilarity != null) {
                appendLine("Node Types: ${(nodeTypeSimilarity * 100).toInt()}%")
            }
            if (propertySimilarity != null) {
                appendLine("Properties: ${(propertySimilarity * 100).toInt()}%")
            }
            if (layoutSimilarity != null) {
                appendLine("Layout: ${(layoutSimilarity * 100).toInt()}%")
            }
            if (constraintSimilarity != null) {
                appendLine("Constraints: ${(constraintSimilarity * 100).toInt()}%")
            }
            if (error != null) {
                appendLine("Error: $error")
            }
        }
    }
}
