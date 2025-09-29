package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.models.ForgeConfiguration
import com.forge.storage.ConfigurationStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Main service for The Forge plugin
 * 
 * Coordinates all plugin functionality including:
 * - Configuration management
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
        configuration = configStorage.loadConfiguration()
        logger.info("Configuration loaded: ${configuration?.let { "enabled" } ?: "default"}")
    }
    
    fun getConfiguration(): ForgeConfiguration {
        return configuration ?: ForgeConfiguration.getDefault()
    }
    
    fun updateConfiguration(newConfig: ForgeConfiguration) {
        configuration = newConfig
        val configStorage = project.getService(ConfigurationStorage::class.java)
        configStorage.saveConfiguration(newConfig)
        logger.info("Configuration updated")
    }
    
    fun isPluginEnabled(): Boolean {
        return configuration?.isEnabled ?: false
    }
    
    fun getServiceScope(): CoroutineScope = serviceScope
    
    fun dispose() {
        serviceScope.cancel()
        logger.info("ForgeService disposed")
    }
}
