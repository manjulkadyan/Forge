package com.forge.models

/**
 * Configuration model for The Forge plugin
 * 
 * Contains all user-configurable settings including:
 * - Figma API credentials
 * - Comparison thresholds
 * - UI preferences
 */
data class ForgeConfiguration(
    val isEnabled: Boolean = true,
    val figmaPersonalAccessToken: String = "",
    val figmaUsername: String = "",
    val visualThreshold: Double = 0.95, // 95% similarity threshold for visual comparison
    val structuralThreshold: Double = 0.90, // 90% similarity threshold for structural comparison
    val autoCaptureOnSave: Boolean = false,
    val showDetailedReports: Boolean = true,
    val maxImageSize: Int = 2048, // Maximum image size for processing
    val cacheDuration: Long = 3600000, // 1 hour cache duration in milliseconds
    val enableDebugLogging: Boolean = false
) {
    
    companion object {
        fun getDefault(): ForgeConfiguration {
            return ForgeConfiguration()
        }
    }
    
    fun isValid(): Boolean {
        return figmaPersonalAccessToken.isNotBlank() && 
               figmaUsername.isNotBlank() &&
               visualThreshold in 0.0..1.0 &&
               structuralThreshold in 0.0..1.0 &&
               maxImageSize > 0 &&
               cacheDuration > 0
    }
    
    fun getVisualThresholdPercentage(): Int {
        return (visualThreshold * 100).toInt()
    }
    
    fun getStructuralThresholdPercentage(): Int {
        return (structuralThreshold * 100).toInt()
    }
}
