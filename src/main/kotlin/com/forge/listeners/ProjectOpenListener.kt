package com.forge.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.services.ForgeService

/**
 * Listener for project lifecycle events
 * 
 * Handles:
 * - Plugin initialization when projects are opened
 * - Cleanup when projects are closed
 * - Project-specific configuration loading
 */
class ProjectOpenListener : ProjectManagerListener {
    
    private val logger = thisLogger()
    
    override fun projectOpened(project: Project) {
        logger.info("Project opened: ${project.name}")
        
        // Initialize Forge services for this project
        try {
            val forgeService = project.getService(ForgeService::class.java)
            forgeService.initialize()
            logger.info("Forge services initialized for project: ${project.name}")
        } catch (e: Exception) {
            logger.error("Failed to initialize Forge services for project: ${project.name}", e)
        }
    }
    
    override fun projectClosed(project: Project) {
        logger.info("Project closed: ${project.name}")
        
        // Cleanup Forge services for this project
        try {
            val forgeService = project.getService(ForgeService::class.java)
            forgeService.dispose()
            logger.info("Forge services disposed for project: ${project.name}")
        } catch (e: Exception) {
            logger.error("Failed to dispose Forge services for project: ${project.name}", e)
        }
    }
}
