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
 * Service for performing visual and structural comparisons
 * 
 * Handles:
 * - Visual comparison using perceptual hashing
 * - Structural comparison of layout properties
 * - Generating comparison reports
 * - Managing comparison thresholds
 */
@Service
class ComparisonService(private val project: Project) {
    
    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var configuration: ForgeConfiguration? = null
    private var isInitialized = false
    
    fun initialize() {
        if (isInitialized) {
            logger.warn("ComparisonService already initialized")
            return
        }
        
        try {
            loadConfiguration()
            isInitialized = true
            logger.info("ComparisonService initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize ComparisonService", e)
            throw e
        }
    }
    
    private fun loadConfiguration() {
        val forgeService = project.getService(ForgeService::class.java)
        configuration = forgeService.getConfiguration()
        logger.info("Comparison configuration loaded")
    }
    
    fun getServiceScope(): CoroutineScope = serviceScope
    
    fun dispose() {
        serviceScope.cancel()
        logger.info("ComparisonService disposed")
    }
}
