package com.forge.storage

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.models.ForgeConfiguration
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Persistent storage service for Forge configuration
 * 
 * Uses IntelliJ's built-in persistence mechanism to store
 * plugin configuration across IDE sessions
 */
@Service
@State(
    name = "ForgeConfiguration",
    storages = [Storage("forge-config.xml")]
)
class ConfigurationStorage : PersistentStateComponent<ConfigurationStorage> {
    
    private val logger = thisLogger()
    
    var configuration: ForgeConfiguration = ForgeConfiguration.getDefault()
    
    fun loadConfiguration(): ForgeConfiguration {
        return configuration
    }
    
    fun saveConfiguration(config: ForgeConfiguration) {
        configuration = config
        logger.info("Configuration saved")
    }
    
    override fun getState(): ConfigurationStorage {
        return this
    }
    
    override fun loadState(state: ConfigurationStorage) {
        XmlSerializerUtil.copyBean(state, this)
        logger.info("Configuration state loaded")
    }
}
