package com.forge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.options.ShowSettingsUtil

/**
 * Action to open The Forge plugin settings
 * 
 * This action opens the plugin configuration dialog where users can
 * set their Figma credentials and adjust comparison thresholds.
 */
class OpenSettingsAction : AnAction(), DumbAware {
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "The Forge"
        )
    }
}
