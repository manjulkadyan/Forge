package com.forge.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.forge.models.ForgeConfiguration
import com.forge.services.ForgeService
import com.forge.storage.ConfigurationStorage
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Configuration UI for The Forge plugin settings
 * 
 * Provides a user-friendly interface for configuring:
 * - Figma API credentials
 * - Comparison thresholds
 * - Plugin behavior settings
 */
class ForgeSettingsConfigurable : Configurable {
    
    private var mainPanel: JPanel? = null
    private var figmaTokenField: JBPasswordField? = null
    private var figmaUsernameField: JBTextField? = null
    private var visualThresholdField: ComboBox<Int>? = null
    private var structuralThresholdField: ComboBox<Int>? = null
    private var autoCaptureCheckBox: JBCheckBox? = null
    private var detailedReportsCheckBox: JBCheckBox? = null
    private var debugLoggingCheckBox: JBCheckBox? = null
    
    private var originalConfig: ForgeConfiguration? = null
    
    override fun getDisplayName(): String = "The Forge"
    
    override fun createComponent(): JComponent? {
        if (mainPanel == null) {
            mainPanel = createMainPanel()
        }
        return mainPanel
    }
    
    private fun createMainPanel(): JPanel {
        // Initialize components
        figmaTokenField = JBPasswordField()
        figmaUsernameField = JBTextField()
        
        // Create threshold combo boxes
        val thresholdOptions = (50..100).toList()
        visualThresholdField = ComboBox(thresholdOptions.toTypedArray())
        structuralThresholdField = ComboBox(thresholdOptions.toTypedArray())
        
        // Create checkboxes
        autoCaptureCheckBox = JBCheckBox("Automatically capture on save")
        detailedReportsCheckBox = JBCheckBox("Show detailed comparison reports")
        debugLoggingCheckBox = JBCheckBox("Enable debug logging")
        
        // Build the form
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Figma Personal Access Token:"), figmaTokenField!!, 1, false)
            .addTooltip("Your Figma Personal Access Token. Get it from Figma > Settings > Account > Personal Access Tokens")
            .addLabeledComponent(JBLabel("Figma Username:"), figmaUsernameField!!, 1, false)
            .addTooltip("Your Figma username (used for secure storage)")
            .addSeparator()
            .addLabeledComponent(JBLabel("Visual Similarity Threshold:"), visualThresholdField!!, 1, false)
            .addTooltip("Minimum percentage similarity required for visual comparison to pass (50-100%)")
            .addLabeledComponent(JBLabel("Structural Similarity Threshold:"), structuralThresholdField!!, 1, false)
            .addTooltip("Minimum percentage similarity required for structural comparison to pass (50-100%)")
            .addSeparator()
            .addComponent(autoCaptureCheckBox!!)
            .addComponent(detailedReportsCheckBox!!)
            .addComponent(debugLoggingCheckBox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel.apply {
                border = JBUI.Borders.empty(10)
            }
    }
    
    override fun isModified(): Boolean {
        val currentConfig = getCurrentConfiguration()
        return currentConfig != originalConfig
    }
    
    override fun apply() {
        val newConfig = getCurrentConfiguration()
        
        // Update all open projects
        ProjectManager.getInstance().openProjects.forEach { project ->
            val forgeService = project.getService(ForgeService::class.java)
            forgeService.updateConfiguration(newConfig)
        }
        
        originalConfig = newConfig
    }
    
    override fun reset() {
        loadConfiguration()
    }
    
    private fun loadConfiguration() {
        // Load from the first available project or use default
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        val config = if (project != null) {
            val forgeService = project.getService(ForgeService::class.java)
            forgeService.getConfiguration()
        } else {
            ForgeConfiguration.getDefault()
        }
        
        // Populate UI components
        figmaTokenField?.text = config.figmaPersonalAccessToken
        figmaUsernameField?.text = config.figmaUsername
        visualThresholdField?.selectedItem = config.getVisualThresholdPercentage()
        structuralThresholdField?.selectedItem = config.getStructuralThresholdPercentage()
        autoCaptureCheckBox?.isSelected = config.autoCaptureOnSave
        detailedReportsCheckBox?.isSelected = config.showDetailedReports
        debugLoggingCheckBox?.isSelected = config.enableDebugLogging
        
        originalConfig = config
    }
    
    private fun getCurrentConfiguration(): ForgeConfiguration {
        return ForgeConfiguration(
            isEnabled = true,
            figmaPersonalAccessToken = String(figmaTokenField?.password ?: charArrayOf()),
            figmaUsername = figmaUsernameField?.text ?: "",
            visualThreshold = (visualThresholdField?.selectedItem as? Int)?.div(100.0) ?: 0.95,
            structuralThreshold = (structuralThresholdField?.selectedItem as? Int)?.div(100.0) ?: 0.90,
            autoCaptureOnSave = autoCaptureCheckBox?.isSelected ?: false,
            showDetailedReports = detailedReportsCheckBox?.isSelected ?: true,
            enableDebugLogging = debugLoggingCheckBox?.isSelected ?: false
        )
    }
}
