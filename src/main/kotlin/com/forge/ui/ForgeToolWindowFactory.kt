package com.forge.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating The Forge tool window
 * 
 * Provides a dedicated UI panel for displaying:
 * - Comparison results
 * - Visual differences
 * - Structural analysis reports
 * - Baseline management controls
 */
class ForgeToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = ForgeToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(toolWindowPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean {
        // Show the tool window for all projects (will be filtered later for Android-specific features)
        return true
    }
}
