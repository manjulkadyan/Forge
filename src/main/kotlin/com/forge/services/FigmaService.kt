package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.models.ForgeConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Service for interacting with the Figma API
 * 
 * Handles:
 * - Authentication with Figma Personal Access Token
 * - Fetching design data (images and structure)
 * - Parsing Figma URLs and node IDs
 * - Caching API responses
 */
@Service
class FigmaService(private val project: Project) {
    
    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var configuration: ForgeConfiguration? = null
    private var isInitialized = false
    
    fun initialize() {
        if (isInitialized) {
            logger.warn("FigmaService already initialized")
            return
        }
        
        try {
            loadConfiguration()
            isInitialized = true
            logger.info("FigmaService initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize FigmaService", e)
            throw e
        }
    }
    
    private fun loadConfiguration() {
        val forgeService = project.getService(ForgeService::class.java)
        configuration = forgeService.getConfiguration()
        logger.info("Figma configuration loaded")
    }
    
    fun getServiceScope(): CoroutineScope = serviceScope
    
    fun dispose() {
        serviceScope.cancel()
        logger.info("FigmaService disposed")
    }
}
