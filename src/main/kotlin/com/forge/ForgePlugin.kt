package com.forge

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.forge.services.ForgeService
import com.forge.services.FigmaService
import com.forge.services.ComparisonService
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Main plugin class for The Forge - Figma to Compose Parity Plugin
 * 
 * This plugin provides automated verification that Compose UI components
 * match their Figma designs with pixel-perfect accuracy and structural validation.
 */
@Service
class ForgePlugin : StartupActivity {
    
    private val logger = thisLogger()
    
    override fun runActivity(project: Project) {
        logger.info("The Forge plugin is starting up for project: ${project.name}")
        
        // Initialize core services
        initializeServices(project)
        
        logger.info("The Forge plugin initialization completed")
    }
    
    private fun initializeServices(project: Project) {
        try {
            // Initialize the main Forge service
            val forgeService = project.getService(ForgeService::class.java)
            forgeService.initialize()
            
            // Initialize Figma API service
            val figmaService = project.getService(FigmaService::class.java)
            figmaService.initialize()
            
            // Initialize comparison engine
            val comparisonService = project.getService(ComparisonService::class.java)
            comparisonService.initialize()
            
            logger.info("All Forge services initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Forge services", e)
        }
    }
}
