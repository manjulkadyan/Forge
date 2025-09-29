package com.forge.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.forge.utils.ComposableAnalyzer
import com.forge.services.ComparisonResult
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.BorderFactory
import java.awt.BorderLayout
import java.awt.GridLayout

/**
 * Dialog for displaying Figma comparison results
 * 
 * Shows users:
 * - Comparison results and scores
 * - Detailed analysis reports
 * - Visual and structural differences
 * - Recommendations for improvements
 */
class ComparisonDialog(
    private val project: Project,
    private val composableInfo: ComposableAnalyzer.ComposableInfo,
    private val comparisonResult: ComparisonResult? = null
) : DialogWrapper(project) {
    
    private var resultTextArea: JBTextArea? = null
    private var summaryLabel: JBLabel? = null
    
    init {
        title = "Compare with Figma - ${composableInfo.functionName}"
        init()
    }
    
    override fun createCenterPanel(): JComponent? {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.border = JBUI.Borders.empty(10)
        
        if (comparisonResult != null) {
            // Show comparison results
            val summaryPanel = JPanel()
            summaryPanel.layout = GridLayout(1, 1)
            
            val summaryText = if (comparisonResult.overallPassed) {
                "✅ Comparison PASSED - ${comparisonResult.getSummary()}"
            } else {
                "❌ Comparison FAILED - ${comparisonResult.getSummary()}"
            }
            
            summaryLabel = JBLabel(summaryText)
            summaryLabel?.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            summaryPanel.add(summaryLabel)
            
            // Detailed results
            resultTextArea = JBTextArea(15, 60)
            resultTextArea?.isEditable = false
            resultTextArea?.text = comparisonResult.getDetailedReport()
            resultTextArea?.border = BorderFactory.createTitledBorder("Detailed Comparison Report")
            
            val scrollPane = JScrollPane(resultTextArea)
            scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            
            panel.add(summaryPanel, BorderLayout.NORTH)
            panel.add(scrollPane, BorderLayout.CENTER)
            
        } else {
            // Show input form for comparison
            val inputPanel = JPanel()
            inputPanel.layout = GridLayout(3, 1, 10, 10)
            
            val infoLabel = JBLabel("Composable: ${composableInfo.functionName}")
            inputPanel.add(infoLabel)
            
            val urlLabel = JBLabel("Figma URL or Node ID:")
            val figmaUrlField = JBTextField()
            figmaUrlField.toolTipText = "Enter Figma URL or node ID to compare with"
            inputPanel.add(urlLabel)
            inputPanel.add(figmaUrlField)
            
            val infoTextArea = JBTextArea(5, 40)
            infoTextArea.isEditable = false
            infoTextArea.text = """
                This will compare your Compose component with the Figma design.
                
                Supported formats:
                - Figma URL: https://www.figma.com/file/...
                - Node ID: node-id-string
                
                The comparison will analyze:
                - Visual similarity (colors, layout, typography)
                - Structural similarity (component hierarchy, properties)
            """.trimIndent()
            inputPanel.add(infoTextArea)
            
            panel.add(inputPanel, BorderLayout.CENTER)
        }
        
        return panel
    }
    
    override fun doOKAction() {
        if (comparisonResult != null) {
            // If showing results, just close the dialog
            super.doOKAction()
        } else {
            // If input form, start comparison process
            // TODO: Get figmaUrl from input field and start comparison process
            super.doOKAction()
        }
    }
}
