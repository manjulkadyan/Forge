package com.forge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.*
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.services.ForgeService
import com.forge.services.ComparisonService
import com.forge.services.ComposeRenderService
import com.forge.ui.ComparisonDialog
import com.forge.utils.ComposableAnalyzer
import com.forge.rendering.RenderResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Action to compare a Composable function with its Figma design
 * 
 * This action is triggered when the user right-clicks on a @Composable @Preview
 * function and selects "Compare with Figma" from the context menu.
 */
class CompareWithFigmaAction : AnAction(), DumbAware {
    
    private val logger = thisLogger()
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        
        try {
            // Check if plugin is enabled and configured
            val forgeService = project.getService(ForgeService::class.java)
            if (!forgeService.isPluginEnabled()) {
                Messages.showMessageDialog(
                    project,
                    "The Forge plugin is not enabled. Please enable it in Settings > Tools > The Forge.",
                    "Plugin Disabled",
                    Messages.getWarningIcon()
                )
                return
            }
            
            val config = forgeService.getConfiguration()
            if (!config.isValid()) {
                Messages.showMessageDialog(
                    project,
                    "The Forge plugin is not properly configured. Please check your Figma credentials in Settings > Tools > The Forge.",
                    "Configuration Required",
                    Messages.getWarningIcon()
                )
                return
            }
            
            // Analyze the current Composable function
            val composableInfo = ComposableAnalyzer.analyzeComposableAtCaret(editor, psiFile)
            if (composableInfo == null) {
                Messages.showMessageDialog(
                    project,
                    "No valid @Composable @Preview function found at the current cursor position.",
                    "No Composable Found",
                    Messages.getWarningIcon()
                )
                return
            }
            
            logger.info("Starting comparison for Composable: ${composableInfo.functionName}")
            
            // Render the Compose component first
            val renderService = project.getService(ComposeRenderService::class.java)
            val renderResult = runBlocking {
                renderService.renderComposable(composableInfo.fullQualifiedName)
            }
            
            if (!renderResult.success) {
                Messages.showMessageDialog(
                    project,
                    "Failed to render Compose component: ${renderResult.error}",
                    "Render Failed",
                    Messages.getErrorIcon()
                )
                return
            }
            
            logger.info("Compose component rendered successfully")
            
            // Show the comparison dialog
            val dialog = ComparisonDialog(project, composableInfo)
            dialog.show()
            
        } catch (e: Exception) {
            logger.error("Error during Figma comparison", e)
            Messages.showErrorDialog(
                project,
                "An error occurred while comparing with Figma: ${e.message}",
                "Comparison Error"
            )
        }
    }
    
    override fun update(event: AnActionEvent) {
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        
        // Enable the action only if we're in a Kotlin file with a valid Composable
        val isEnabled = project != null && 
                       editor != null && 
                       psiFile != null && 
                       psiFile.name.endsWith(".kt") &&
                       ComposableAnalyzer.hasComposableAtCaret(editor, psiFile)
        
        event.presentation.isEnabledAndVisible = isEnabled
    }
}
