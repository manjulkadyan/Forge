package com.forge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.*
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.services.ForgeService
import com.forge.services.BaselineService
import com.forge.utils.ComposableAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Action to approve the current Compose render as a new baseline
 * 
 * This action allows developers to approve intentional design changes
 * and update the stored baseline for future comparisons.
 */
class ApproveAsBaselineAction : AnAction(), DumbAware {
    
    private val logger = thisLogger()
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        
        try {
            // Check if plugin is enabled
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
            
            // Confirm the action with the user
            val result = Messages.showYesNoDialog(
                "Are you sure you want to approve the current Compose render as the new baseline for '${composableInfo.functionName}'?\n\nThis will update the stored baseline and may affect future comparisons.",
                "Approve as Baseline",
                "Approve",
                "Cancel",
                null
            )
            
            if (result == Messages.YES) {
                val baselineService = project.getService(BaselineService::class.java)
                val serviceScope = forgeService.getServiceScope()
                
                serviceScope.launch {
                    try {
                        baselineService.approveAsBaseline(composableInfo)
                        Messages.showInfoMessage(
                            project,
                            "Baseline updated successfully for '${composableInfo.functionName}'",
                            "Baseline Updated"
                        )
                        logger.info("Baseline approved for Composable: ${composableInfo.functionName}")
                    } catch (e: Exception) {
                        logger.error("Error approving baseline", e)
                        Messages.showErrorDialog(
                            project,
                            "Failed to update baseline: ${e.message}",
                            "Baseline Update Error"
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error("Error during baseline approval", e)
            Messages.showErrorDialog(
                project,
                "An error occurred while approving baseline: ${e.message}",
                "Baseline Approval Error"
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
