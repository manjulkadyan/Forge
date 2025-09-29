package com.forge.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.forge.utils.ComposableAnalyzer
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * Dialog for initiating Figma comparison
 * 
 * Allows users to:
 * - Enter Figma URL or node ID
 * - Configure comparison options
 * - Start the comparison process
 */
class ComparisonDialog(
    private val project: Project,
    private val composableInfo: ComposableAnalyzer.ComposableInfo
) : DialogWrapper(project) {
    
    private var figmaUrlField: JBTextField? = null
    private var infoTextArea: JTextArea? = null
    
    init {
        title = "Compare with Figma - ${composableInfo.functionName}"
        init()
    }
    
    override fun createCenterPanel(): JComponent? {
        val panel = JPanel()
        panel.border = JBUI.Borders.empty(10)
        
        // Composable info
        infoTextArea = JTextArea(4, 50).apply {
            isEditable = false
            text = buildString {
                appendLine("Composable Function: ${composableInfo.functionName}")
                appendLine("Package: ${composableInfo.packageName ?: "N/A"}")
                appendLine("Class: ${composableInfo.className ?: "N/A"}")
                appendLine("Location: Line ${composableInfo.lineNumber}")
            }
        }
        
        // Figma URL input
        figmaUrlField = JBTextField().apply {
            toolTipText = "Enter Figma URL or node ID (e.g., https://figma.com/design/abc123/My-Design?node-id=1-2)"
        }
        
        panel.add(JBLabel("Figma URL or Node ID:"))
        panel.add(figmaUrlField)
        panel.add(JBLabel("Composable Information:"))
        panel.add(infoTextArea)
        
        return panel
    }
    
    override fun doOKAction() {
        val figmaUrl = figmaUrlField?.text?.trim()
        if (figmaUrl.isNullOrBlank()) {
            // TODO: Show error message
            return
        }
        
        // TODO: Start comparison process
        super.doOKAction()
    }
}
