package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.models.ForgeConfiguration
import com.forge.models.ConfigurationValidationResult
import com.forge.storage.ConfigurationStorage
import com.forge.security.ForgeCredentialManager
import com.forge.security.CredentialStorageException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Main service for The Forge plugin
 * 
 * Coordinates all plugin functionality including:
 * - Configuration management with secure credential storage
 * - Project state tracking
 * - Service orchestration
 */
@Service
class ForgeService(private val project: Project) {
    
    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var configuration: ForgeConfiguration? = null
    private var isInitialized = false
    
    fun initialize() {
        if (isInitialized) {
            logger.warn("ForgeService already initialized")
            return
        }
        
        try {
            loadConfiguration()
            isInitialized = true
            logger.info("ForgeService initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize ForgeService", e)
            throw e
        }
    }
    
    private fun loadConfiguration() {
        val configStorage = project.getService(ConfigurationStorage::class.java)
        val baseConfig = configStorage.loadConfiguration()
        
        // If secure storage is enabled, try to load credentials from PasswordSafe
        if (baseConfig.useSecureStorage) {
            val credentialManager = project.getService(ForgeCredentialManager::class.java)
            val storedUsername = credentialManager.getFigmaUsername()
            val storedToken = credentialManager.getFigmaToken(storedUsername)
            
            if (storedUsername != null && storedToken != null) {
                configuration = baseConfig.copy(
                    figmaUsername = storedUsername,
                    figmaPersonalAccessToken = storedToken
                )
                logger.info("Configuration loaded with secure credentials for user: $storedUsername")
            } else {
                configuration = baseConfig
                logger.info("Configuration loaded without secure credentials")
            }
        } else {
            configuration = baseConfig
            logger.info("Configuration loaded: ${baseConfig.toSafeString()}")
        }
    }
    
    fun getConfiguration(): ForgeConfiguration {
        return configuration ?: ForgeConfiguration.getDefault()
    }
    
    fun updateConfiguration(newConfig: ForgeConfiguration) {
        try {
            // Validate configuration before saving
            val validation = newConfig.validate()
            if (!validation.isValid) {
                logger.error("Configuration validation failed: ${validation.getErrorMessage()}")
                throw IllegalArgumentException("Invalid configuration: ${validation.getErrorMessage()}")
            }
            
            // Handle secure storage of credentials
            if (newConfig.useSecureStorage && newConfig.hasValidCredentials()) {
                val credentialManager = project.getService(ForgeCredentialManager::class.java)
                credentialManager.storeFigmaToken(newConfig.figmaPersonalAccessToken, newConfig.figmaUsername)
                
                // Store configuration without sensitive data
                val safeConfig = newConfig.copy(
                    figmaPersonalAccessToken = "",
                    figmaUsername = ""
                )
                val configStorage = project.getService(ConfigurationStorage::class.java)
                configStorage.saveConfiguration(safeConfig)
                
                configuration = newConfig
                logger.info("Configuration updated with secure credential storage")
            } else {
                // Store configuration normally (not recommended for production)
                val configStorage = project.getService(ConfigurationStorage::class.java)
                configStorage.saveConfiguration(newConfig)
                configuration = newConfig
                logger.info("Configuration updated: ${newConfig.toSafeString()}")
            }
        } catch (e: CredentialStorageException) {
            logger.error("Failed to store credentials securely", e)
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update configuration", e)
            throw e
        }
    }
    
    fun validateConfiguration(): ConfigurationValidationResult {
        val currentConfig = getConfiguration()
        return currentConfig.validate()
    }
    
    fun isPluginEnabled(): Boolean {
        return configuration?.isEnabled ?: false
    }
    
    fun hasValidCredentials(): Boolean {
        return configuration?.hasValidCredentials() ?: false
    }
    
    fun clearCredentials() {
        try {
            val credentialManager = project.getService(ForgeCredentialManager::class.java)
            credentialManager.clearCredentials()
            
            // Update configuration to remove credentials
            val currentConfig = getConfiguration()
            val updatedConfig = currentConfig.copy(
                figmaPersonalAccessToken = "",
                figmaUsername = ""
            )
            updateConfiguration(updatedConfig)
            
            logger.info("Credentials cleared successfully")
        } catch (e: Exception) {
            logger.error("Failed to clear credentials", e)
            throw e
        }
    }
    
    fun testFigmaConnection(): Boolean {
        val currentConfig = getConfiguration()
        if (!currentConfig.hasValidCredentials()) {
            return false
        }
        
        return try {
            val credentialManager = project.getService(ForgeCredentialManager::class.java)
            serviceScope.launch {
                val isValid = credentialManager.validateTokenWithFigma(currentConfig.figmaPersonalAccessToken)
                if (isValid) {
                    logger.info("Figma connection test successful")
                } else {
                    logger.warn("Figma connection test failed - invalid token")
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Figma connection test failed", e)
            false
        }
    }
    
    fun getServiceScope(): CoroutineScope = serviceScope
    
    fun dispose() {
        serviceScope.cancel()
        logger.info("ForgeService disposed")
    }
}
