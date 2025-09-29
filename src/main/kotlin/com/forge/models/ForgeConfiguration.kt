package com.forge.models

import com.forge.security.ForgeCredentialManager

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
    val enableDebugLogging: Boolean = false,
    val useSecureStorage: Boolean = true, // Whether to use PasswordSafe for credentials
    val authenticationMethod: AuthenticationMethod = AuthenticationMethod.AUTO, // How to authenticate with Figma
    val mcpServerPath: String = "", // Path to MCP server if using MCP
    val preferMCP: Boolean = true // Prefer MCP over direct API when available
) {
    
    companion object {
        fun getDefault(): ForgeConfiguration {
            return ForgeConfiguration()
        }
    }
    
    /**
     * Validate the configuration and return detailed validation results
     */
    fun validate(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate Figma credentials based on authentication method
        when (authenticationMethod) {
            AuthenticationMethod.PAT_ONLY -> {
                if (figmaUsername.isBlank()) {
                    errors.add("Figma username is required for PAT authentication")
                } else if (!isValidUsername(figmaUsername)) {
                    errors.add("Figma username contains invalid characters")
                }
                
                if (figmaPersonalAccessToken.isBlank()) {
                    errors.add("Figma Personal Access Token is required for PAT authentication")
                } else if (!ForgeCredentialManager.validateTokenFormat(figmaPersonalAccessToken)) {
                    errors.add("Figma Personal Access Token format is invalid (should start with 'figd_' and be 64 characters)")
                }
            }
            AuthenticationMethod.MCP_ONLY -> {
                if (mcpServerPath.isBlank()) {
                    errors.add("MCP server path is required for MCP authentication")
                } else if (!isValidMcpPath(mcpServerPath)) {
                    errors.add("MCP server path is invalid")
                }
            }
            AuthenticationMethod.AUTO -> {
                // At least one authentication method should be configured
                val hasPat = figmaPersonalAccessToken.isNotBlank() && ForgeCredentialManager.validateTokenFormat(figmaPersonalAccessToken)
                val hasMcp = mcpServerPath.isNotBlank() && isValidMcpPath(mcpServerPath)
                
                if (!hasPat && !hasMcp) {
                    errors.add("Either Figma PAT or MCP server path is required for AUTO authentication")
                }
                
                if (figmaUsername.isNotBlank() && !isValidUsername(figmaUsername)) {
                    errors.add("Figma username contains invalid characters")
                }
            }
            AuthenticationMethod.NONE -> {
                warnings.add("No authentication configured - some features may not work")
            }
        }
        
        // Validate thresholds
        if (visualThreshold !in 0.0..1.0) {
            errors.add("Visual threshold must be between 0.0 and 1.0")
        } else if (visualThreshold < 0.8) {
            warnings.add("Visual threshold below 80% may result in false positives")
        }
        
        if (structuralThreshold !in 0.0..1.0) {
            errors.add("Structural threshold must be between 0.0 and 1.0")
        } else if (structuralThreshold < 0.7) {
            warnings.add("Structural threshold below 70% may result in false positives")
        }
        
        // Validate image size
        if (maxImageSize <= 0) {
            errors.add("Maximum image size must be positive")
        } else if (maxImageSize > 4096) {
            warnings.add("Large image sizes may impact performance")
        }
        
        // Validate cache duration
        if (cacheDuration <= 0) {
            errors.add("Cache duration must be positive")
        } else if (cacheDuration > 86400000) { // 24 hours
            warnings.add("Cache duration longer than 24 hours may use excessive storage")
        }
        
        return ConfigurationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Check if basic configuration is valid (for UI state)
     */
    fun isValid(): Boolean {
        return validate().isValid
    }
    
    /**
     * Check if credentials are properly configured
     */
    fun hasValidCredentials(): Boolean {
        return figmaPersonalAccessToken.isNotBlank() && 
               figmaUsername.isNotBlank() &&
               ForgeCredentialManager.validateTokenFormat(figmaPersonalAccessToken)
    }
    
    /**
     * Get a safe version of the configuration for logging (without sensitive data)
     */
    fun toSafeString(): String {
        return "ForgeConfiguration(" +
                "isEnabled=$isEnabled, " +
                "figmaUsername='$figmaUsername', " +
                "figmaToken='${if (figmaPersonalAccessToken.isNotBlank()) "***" else "empty"}', " +
                "visualThreshold=$visualThreshold, " +
                "structuralThreshold=$structuralThreshold, " +
                "autoCaptureOnSave=$autoCaptureOnSave, " +
                "showDetailedReports=$showDetailedReports, " +
                "maxImageSize=$maxImageSize, " +
                "cacheDuration=$cacheDuration, " +
                "enableDebugLogging=$enableDebugLogging, " +
                "useSecureStorage=$useSecureStorage" +
                ")"
    }
    
    fun getVisualThresholdPercentage(): Int {
        return (visualThreshold * 100).toInt()
    }
    
    fun getStructuralThresholdPercentage(): Int {
        return (structuralThreshold * 100).toInt()
    }
    
    private fun isValidUsername(username: String): Boolean {
        // Figma usernames are typically alphanumeric with some special characters
        return username.matches(Regex("^[a-zA-Z0-9._-]+$")) && username.length in 1..50
    }
    
    private fun isValidMcpPath(path: String): Boolean {
        // MCP server path should be a valid executable path
        return path.isNotBlank() && (path.endsWith(".exe") || path.endsWith(".sh") || path.endsWith(".py") || path.endsWith(".js"))
    }
}

/**
 * Authentication methods for Figma API access
 */
enum class AuthenticationMethod {
    AUTO,           // Try MCP first, fallback to PAT
    MCP_ONLY,       // Use MCP only
    PAT_ONLY,       // Use Personal Access Token only
    NONE            // No authentication (for testing)
}

/**
 * Result of configuration validation
 */
data class ConfigurationValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    
    fun getErrorMessage(): String = errors.joinToString("\n")
    fun getWarningMessage(): String = warnings.joinToString("\n")
}
