package com.forge.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.forge.models.ForgeConfiguration
import com.forge.models.ConfigurationValidationResult
import com.forge.models.AuthenticationMethod
import com.forge.services.ForgeService
import com.forge.security.ForgeCredentialManager
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton
import java.awt.BorderLayout
import java.awt.FlowLayout

/**
 * Enhanced configuration UI for The Forge plugin settings
 * 
 * Provides a user-friendly interface for configuring:
 * - Figma API credentials with secure storage
 * - Comparison thresholds with validation
 * - Plugin behavior settings
 * - Real-time validation and feedback
 */
class ForgeSettingsConfigurable : Configurable {
    
    private var mainPanel: JPanel? = null
    private var figmaTokenField: JBPasswordField? = null
    private var figmaUsernameField: JBTextField? = null
    private var visualThresholdField: com.intellij.openapi.ui.ComboBox<Int>? = null
    private var structuralThresholdField: com.intellij.openapi.ui.ComboBox<Int>? = null
    private var autoCaptureCheckBox: JBCheckBox? = null
    private var detailedReportsCheckBox: JBCheckBox? = null
    private var debugLoggingCheckBox: JBCheckBox? = null
    private var useSecureStorageCheckBox: JBCheckBox? = null
    private var authenticationMethodCombo: com.intellij.openapi.ui.ComboBox<com.forge.models.AuthenticationMethod>? = null
    private var mcpServerPathField: JBTextField? = null
    private var preferMcpCheckBox: JBCheckBox? = null
    private var testConnectionButton: JButton? = null
    private var clearCredentialsButton: JButton? = null
    private var validationTextArea: JBTextArea? = null
    
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
        figmaTokenField = JBPasswordField().apply {
            toolTipText = "Your Figma Personal Access Token. Get it from Figma > Settings > Account > Personal Access Tokens"
        }
        figmaUsernameField = JBTextField().apply {
            toolTipText = "Your Figma username (used for secure storage and identification)"
        }
        
        // Create threshold combo boxes
        val thresholdOptions = (50..100).toList()
        visualThresholdField = com.intellij.openapi.ui.ComboBox(thresholdOptions.toTypedArray()).apply {
            toolTipText = "Minimum percentage similarity required for visual comparison to pass (50-100%)"
        }
        structuralThresholdField = com.intellij.openapi.ui.ComboBox(thresholdOptions.toTypedArray()).apply {
            toolTipText = "Minimum percentage similarity required for structural comparison to pass (50-100%)"
        }
        
        // Create checkboxes
        autoCaptureCheckBox = JBCheckBox("Automatically capture on save").apply {
            toolTipText = "Automatically capture Compose renders when files are saved"
        }
        detailedReportsCheckBox = JBCheckBox("Show detailed comparison reports").apply {
            toolTipText = "Display detailed analysis in comparison results"
        }
        debugLoggingCheckBox = JBCheckBox("Enable debug logging").apply {
            toolTipText = "Enable detailed logging for troubleshooting"
        }
        useSecureStorageCheckBox = JBCheckBox("Use secure storage for credentials").apply {
            toolTipText = "Store Figma credentials securely using IntelliJ's PasswordSafe (recommended)"
            isSelected = true
        }
        
        // Authentication method selection
        authenticationMethodCombo = com.intellij.openapi.ui.ComboBox(AuthenticationMethod.values()).apply {
            toolTipText = "Choose how to authenticate with Figma"
        }
        
        mcpServerPathField = JBTextField().apply {
            toolTipText = "Path to MCP server executable (e.g., /path/to/figma-mcp-server)"
        }
        
        preferMcpCheckBox = JBCheckBox("Prefer MCP over direct API").apply {
            toolTipText = "Use MCP when available, fallback to direct API"
            isSelected = true
        }
        
        // Create buttons
        testConnectionButton = JButton("Test Figma Connection").apply {
            addActionListener { testFigmaConnection() }
        }
        clearCredentialsButton = JButton("Clear Credentials").apply {
            addActionListener { clearCredentials() }
        }
        
        // Create validation display
        validationTextArea = JBTextArea(3, 50).apply {
            isEditable = false
            isOpaque = false
            text = "Configuration validation will appear here..."
        }
        
        // Build the form
        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Authentication Method:"), authenticationMethodCombo!!, 1, false)
            .addLabeledComponent(JBLabel("Figma Personal Access Token:"), figmaTokenField!!, 1, false)
            .addLabeledComponent(JBLabel("Figma Username:"), figmaUsernameField!!, 1, false)
            .addLabeledComponent(JBLabel("MCP Server Path:"), mcpServerPathField!!, 1, false)
            .addComponent(preferMcpCheckBox!!)
            .addComponent(useSecureStorageCheckBox!!)
            .addSeparator()
            .addLabeledComponent(JBLabel("Visual Similarity Threshold:"), visualThresholdField!!, 1, false)
            .addLabeledComponent(JBLabel("Structural Similarity Threshold:"), structuralThresholdField!!, 1, false)
            .addSeparator()
            .addComponent(autoCaptureCheckBox!!)
            .addComponent(detailedReportsCheckBox!!)
            .addComponent(debugLoggingCheckBox!!)
            .addSeparator()
            .addComponent(createButtonPanel())
            .addSeparator()
            .addLabeledComponent(JBLabel("Validation:"), validationTextArea!!, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel.apply {
                border = JBUI.Borders.empty(10)
            }
        
        return formPanel
    }
    
    private fun createButtonPanel(): JPanel {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(testConnectionButton)
        buttonPanel.add(clearCredentialsButton)
        return buttonPanel
    }
    
    override fun isModified(): Boolean {
        val currentConfig = getCurrentConfiguration()
        return currentConfig != originalConfig
    }
    
    override fun apply() {
        val newConfig = getCurrentConfiguration()
        
        try {
            // Validate configuration before applying
            val validation = newConfig.validate()
            if (!validation.isValid) {
                Messages.showErrorDialog(
                    "Configuration validation failed:\n${validation.getErrorMessage()}",
                    "Invalid Configuration"
                )
                return
            }
            
            // Show warnings if any
            if (validation.hasWarnings()) {
                val result = Messages.showYesNoDialog(
                    "Configuration warnings:\n${validation.getWarningMessage()}\n\nDo you want to continue?",
                    "Configuration Warnings",
                    "Continue",
                    "Cancel",
                    null
                )
                if (result != Messages.YES) {
                    return
                }
            }
            
            // Update all open projects
            ProjectManager.getInstance().openProjects.forEach { project ->
                val forgeService = project.getService(ForgeService::class.java)
                forgeService.updateConfiguration(newConfig)
            }
            
            originalConfig = newConfig
            updateValidationDisplay()
            
            Messages.showInfoMessage(
                "Configuration saved successfully!",
                "Configuration Saved"
            )
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "Failed to save configuration: ${e.message}",
                "Configuration Error"
            )
        }
    }
    
    override fun reset() {
        loadConfiguration()
        updateValidationDisplay()
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
        authenticationMethodCombo?.selectedItem = config.authenticationMethod
        figmaTokenField?.text = config.figmaPersonalAccessToken
        figmaUsernameField?.text = config.figmaUsername
        mcpServerPathField?.text = config.mcpServerPath
        preferMcpCheckBox?.isSelected = config.preferMCP
        visualThresholdField?.selectedItem = config.getVisualThresholdPercentage()
        structuralThresholdField?.selectedItem = config.getStructuralThresholdPercentage()
        autoCaptureCheckBox?.isSelected = config.autoCaptureOnSave
        detailedReportsCheckBox?.isSelected = config.showDetailedReports
        debugLoggingCheckBox?.isSelected = config.enableDebugLogging
        useSecureStorageCheckBox?.isSelected = config.useSecureStorage
        
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
            enableDebugLogging = debugLoggingCheckBox?.isSelected ?: false,
            useSecureStorage = useSecureStorageCheckBox?.isSelected ?: true,
            authenticationMethod = authenticationMethodCombo?.selectedItem as? AuthenticationMethod ?: AuthenticationMethod.AUTO,
            mcpServerPath = mcpServerPathField?.text ?: "",
            preferMCP = preferMcpCheckBox?.isSelected ?: true
        )
    }
    
    private fun testFigmaConnection() {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project == null) {
            Messages.showMessageDialog(
                "No project is currently open",
                "No Project",
                Messages.getWarningIcon()
            )
            return
        }
        
        val forgeService = project.getService(ForgeService::class.java)
        val isValid = forgeService.testFigmaConnection()
        
        if (isValid) {
            Messages.showInfoMessage("Figma connection test successful!", "Connection Test")
        } else {
            Messages.showMessageDialog(
                "Figma connection test failed. Please check your credentials.",
                "Connection Test",
                Messages.getWarningIcon()
            )
        }
    }
    
    private fun clearCredentials() {
        val result = Messages.showYesNoDialog(
            "Are you sure you want to clear all stored Figma credentials?",
            "Clear Credentials",
            "Clear",
            "Cancel",
            null
        )
        
        if (result == Messages.YES) {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val forgeService = project.getService(ForgeService::class.java)
                forgeService.clearCredentials()
                
                // Clear UI fields
                figmaTokenField?.text = ""
                figmaUsernameField?.text = ""
                
                Messages.showInfoMessage("Credentials cleared successfully", "Credentials Cleared")
                updateValidationDisplay()
            }
        }
    }
    
    private fun updateValidationDisplay() {
        val currentConfig = getCurrentConfiguration()
        val validation = currentConfig.validate()
        
        val displayText = buildString {
            if (validation.isValid) {
                append("✅ Configuration is valid")
                if (validation.hasWarnings()) {
                    append("\n⚠️ Warnings:\n${validation.getWarningMessage()}")
                }
            } else {
                append("❌ Configuration has errors:\n${validation.getErrorMessage()}")
            }
        }
        
        validationTextArea?.text = displayText
    }
}
