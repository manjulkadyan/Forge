package com.forge.comparison

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Visual comparison engine using perceptual hashing and image analysis
 * 
 * This engine compares Compose renders with Figma designs using:
 * - Perceptual hashing (pHash) for structural similarity
 * - Color histogram analysis for color accuracy
 * - Edge detection for layout precision
 * - Pixel-level comparison for exact matches
 */
class VisualComparisonEngine {

    private val logger = thisLogger()

    /**
     * Compare two images and return similarity score (0.0 to 1.0)
     */
    suspend fun compareImages(
        composeImageBytes: ByteArray,
        figmaImageBytes: ByteArray,
        threshold: Double = 0.95
    ): VisualComparisonResult = withContext(Dispatchers.IO) {
        try {
            val composeImage = ImageIO.read(ByteArrayInputStream(composeImageBytes))
            val figmaImage = ImageIO.read(ByteArrayInputStream(figmaImageBytes))

            if (composeImage == null || figmaImage == null) {
                return@withContext VisualComparisonResult(
                    similarity = 0.0,
                    passed = false,
                    error = "Failed to load one or both images"
                )
            }

            // Resize images to same dimensions for comparison
            val normalizedCompose = normalizeImage(composeImage)
            val normalizedFigma = normalizeImage(figmaImage)

            // Calculate different similarity metrics
            val perceptualHashSimilarity = calculatePerceptualHashSimilarity(normalizedCompose, normalizedFigma)
            val colorHistogramSimilarity = calculateColorHistogramSimilarity(normalizedCompose, normalizedFigma)
            val edgeSimilarity = calculateEdgeSimilarity(normalizedCompose, normalizedFigma)
            val pixelSimilarity = calculatePixelSimilarity(normalizedCompose, normalizedFigma)

            // Weighted average of all metrics
            val overallSimilarity = (
                perceptualHashSimilarity * 0.3 +
                colorHistogramSimilarity * 0.25 +
                edgeSimilarity * 0.25 +
                pixelSimilarity * 0.2
            )

            val passed = overallSimilarity >= threshold

            logger.info("Visual comparison completed: similarity=$overallSimilarity, passed=$passed")

            VisualComparisonResult(
                similarity = overallSimilarity,
                passed = passed,
                perceptualHashSimilarity = perceptualHashSimilarity,
                colorHistogramSimilarity = colorHistogramSimilarity,
                edgeSimilarity = edgeSimilarity,
                pixelSimilarity = pixelSimilarity,
                threshold = threshold
            )

        } catch (e: Exception) {
            logger.error("Error during visual comparison", e)
            VisualComparisonResult(
                similarity = 0.0,
                passed = false,
                error = "Comparison failed: ${e.message}"
            )
        }
    }

    /**
     * Calculate perceptual hash similarity using simplified pHash algorithm
     */
    private fun calculatePerceptualHashSimilarity(image1: BufferedImage, image2: BufferedImage): Double {
        try {
            val hash1 = calculatePerceptualHash(image1)
            val hash2 = calculatePerceptualHash(image2)
            
            val hammingDistance = calculateHammingDistance(hash1, hash2)
            val maxDistance = 64.0 // 64-bit hash
            return 1.0 - (hammingDistance / maxDistance)
        } catch (e: Exception) {
            logger.warn("Error calculating perceptual hash similarity", e)
            return 0.0
        }
    }

    /**
     * Calculate perceptual hash for an image
     */
    private fun calculatePerceptualHash(image: BufferedImage): Long {
        // Resize to 8x8 for hash calculation
        val resized = resizeImage(image, 8, 8)
        
        // Convert to grayscale
        val grayscale = convertToGrayscale(resized)
        
        // Calculate average pixel value
        var sum = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                sum += (grayscale.getRGB(x, y) and 0xFF).toLong()
            }
        }
        val average = sum / 64
        
        // Create hash based on whether each pixel is above or below average
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val pixel = grayscale.getRGB(x, y) and 0xFF
                if (pixel > average) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        
        return hash
    }

    /**
     * Calculate Hamming distance between two hashes
     */
    private fun calculateHammingDistance(hash1: Long, hash2: Long): Int {
        var distance = 0
        var xor = hash1 xor hash2
        while (xor != 0L) {
            distance++
            xor = xor and (xor - 1)
        }
        return distance
    }

    /**
     * Calculate color histogram similarity
     */
    private fun calculateColorHistogramSimilarity(image1: BufferedImage, image2: BufferedImage): Double {
        try {
            val histogram1 = calculateColorHistogram(image1)
            val histogram2 = calculateColorHistogram(image2)
            
            // Calculate correlation coefficient
            val correlation = calculateCorrelation(histogram1, histogram2)
            return (correlation + 1.0) / 2.0 // Normalize to 0-1
        } catch (e: Exception) {
            logger.warn("Error calculating color histogram similarity", e)
            return 0.0
        }
    }

    /**
     * Calculate color histogram for an image
     */
    private fun calculateColorHistogram(image: BufferedImage): IntArray {
        val histogram = IntArray(256) { 0 }
        
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val red = (rgb shr 16) and 0xFF
                val green = (rgb shr 8) and 0xFF
                val blue = rgb and 0xFF
                
                // Use grayscale value as histogram index
                val grayscale = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()
                histogram[grayscale]++
            }
        }
        
        return histogram
    }

    /**
     * Calculate correlation coefficient between two histograms
     */
    private fun calculateCorrelation(hist1: IntArray, hist2: IntArray): Double {
        val n = hist1.size
        var sum1 = 0.0
        var sum2 = 0.0
        var sum1Sq = 0.0
        var sum2Sq = 0.0
        var pSum = 0.0
        
        for (i in 0 until n) {
            val val1 = hist1[i].toDouble()
            val val2 = hist2[i].toDouble()
            
            sum1 += val1
            sum2 += val2
            sum1Sq += val1 * val1
            sum2Sq += val2 * val2
            pSum += val1 * val2
        }
        
        val num = pSum - (sum1 * sum2 / n)
        val den = sqrt((sum1Sq - sum1 * sum1 / n) * (sum2Sq - sum2 * sum2 / n))
        
        return if (den == 0.0) 0.0 else num / den
    }

    /**
     * Calculate edge similarity using simplified edge detection
     */
    private fun calculateEdgeSimilarity(image1: BufferedImage, image2: BufferedImage): Double {
        try {
            val edges1 = detectEdges(image1)
            val edges2 = detectEdges(image2)
            
            // Calculate similarity between edge maps
            var matches = 0
            var total = 0
            
            for (y in 0 until edges1.height) {
                for (x in 0 until edges1.width) {
                    val edge1 = (edges1.getRGB(x, y) and 0xFF) > 128
                    val edge2 = (edges2.getRGB(x, y) and 0xFF) > 128
                    
                    if (edge1 == edge2) matches++
                    total++
                }
            }
            
            return if (total > 0) matches.toDouble() / total else 0.0
        } catch (e: Exception) {
            logger.warn("Error calculating edge similarity", e)
            return 0.0
        }
    }

    /**
     * Simple edge detection using Sobel operator
     */
    private fun detectEdges(image: BufferedImage): BufferedImage {
        val grayscale = convertToGrayscale(image)
        val edges = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_GRAY)
        
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until grayscale.height - 1) {
            for (x in 1 until grayscale.width - 1) {
                var gx = 0
                var gy = 0
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = grayscale.getRGB(x + kx, y + ky) and 0xFF
                        gx += pixel * sobelX[ky + 1][kx + 1]
                        gy += pixel * sobelY[ky + 1][kx + 1]
                    }
                }
                
                val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt()
                val clampedMagnitude = magnitude.coerceIn(0, 255)
                edges.setRGB(x, y, (clampedMagnitude shl 16) or (clampedMagnitude shl 8) or clampedMagnitude)
            }
        }
        
        return edges
    }

    /**
     * Calculate pixel-level similarity
     */
    private fun calculatePixelSimilarity(image1: BufferedImage, image2: BufferedImage): Double {
        try {
            var matches = 0
            var total = 0
            
            for (y in 0 until image1.height) {
                for (x in 0 until image1.width) {
                    val rgb1 = image1.getRGB(x, y)
                    val rgb2 = image2.getRGB(x, y)
                    
                    // Calculate color difference
                    val r1 = (rgb1 shr 16) and 0xFF
                    val g1 = (rgb1 shr 8) and 0xFF
                    val b1 = rgb1 and 0xFF
                    
                    val r2 = (rgb2 shr 16) and 0xFF
                    val g2 = (rgb2 shr 8) and 0xFF
                    val b2 = rgb2 and 0xFF
                    
                    val colorDiff = sqrt(
                        ((r1 - r2) * (r1 - r2) + 
                         (g1 - g2) * (g1 - g2) + 
                         (b1 - b2) * (b1 - b2)).toDouble()
                    )
                    
                    // Consider pixels similar if color difference is small
                    if (colorDiff < 30) matches++
                    total++
                }
            }
            
            return if (total > 0) matches.toDouble() / total else 0.0
        } catch (e: Exception) {
            logger.warn("Error calculating pixel similarity", e)
            return 0.0
        }
    }

    /**
     * Normalize image to standard size for comparison
     */
    private fun normalizeImage(image: BufferedImage): BufferedImage {
        val targetSize = 512
        return resizeImage(image, targetSize, targetSize)
    }

    /**
     * Resize image to specified dimensions
     */
    private fun resizeImage(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return resized
    }

    /**
     * Convert image to grayscale
     */
    private fun convertToGrayscale(image: BufferedImage): BufferedImage {
        val grayscale = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_GRAY)
        val g = grayscale.createGraphics()
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return grayscale
    }
}

/**
 * Result of visual comparison
 */
data class VisualComparisonResult(
    val similarity: Double,
    val passed: Boolean,
    val perceptualHashSimilarity: Double? = null,
    val colorHistogramSimilarity: Double? = null,
    val edgeSimilarity: Double? = null,
    val pixelSimilarity: Double? = null,
    val threshold: Double? = null,
    val error: String? = null
) {
    fun getDetailedReport(): String {
        return buildString {
            appendLine("Visual Comparison Report:")
            appendLine("Overall Similarity: ${(similarity * 100).toInt()}%")
            appendLine("Passed: $passed")
            if (threshold != null) appendLine("Threshold: ${(threshold * 100).toInt()}%")
            
            if (perceptualHashSimilarity != null) {
                appendLine("Perceptual Hash: ${(perceptualHashSimilarity * 100).toInt()}%")
            }
            if (colorHistogramSimilarity != null) {
                appendLine("Color Histogram: ${(colorHistogramSimilarity * 100).toInt()}%")
            }
            if (edgeSimilarity != null) {
                appendLine("Edge Detection: ${(edgeSimilarity * 100).toInt()}%")
            }
            if (pixelSimilarity != null) {
                appendLine("Pixel-level: ${(pixelSimilarity * 100).toInt()}%")
            }
            if (error != null) {
                appendLine("Error: $error")
            }
        }
    }
}
