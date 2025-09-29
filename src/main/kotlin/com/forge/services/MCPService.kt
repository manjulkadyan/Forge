package com.forge.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.models.ForgeConfiguration
import com.forge.models.AuthenticationMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * MCP (Model Context Protocol) service for automatic Figma authentication
 * 
 * This service handles:
 * - Automatic MCP server discovery and connection
 * - Figma data acquisition through MCP
 * - Fallback to direct API when MCP is not available
 */
@Service
class MCPService(private val project: Project) {
    
    private val logger = thisLogger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var mcpProcess: Process? = null
    private var isConnected = false
    
    fun initialize() {
        logger.info("MCP Service initialized")
    }
    
    /**
     * Check if MCP server is available and can connect to Figma
     */
    suspend fun isAvailable(): Boolean {
        return try {
            val config = getCurrentConfiguration()
            when (config.authenticationMethod) {
                AuthenticationMethod.MCP_ONLY, AuthenticationMethod.AUTO -> {
                    val serverPath = config.mcpServerPath
                    if (serverPath.isBlank()) {
                        false
                    } else {
                        testMcpConnection(serverPath)
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            logger.warn("MCP availability check failed", e)
            false
        }
    }
    
    /**
     * Connect to MCP server
     */
    suspend fun connect(): Boolean {
        return try {
            val config = getCurrentConfiguration()
            if (config.mcpServerPath.isBlank()) {
                logger.warn("No MCP server path configured")
                return false
            }
            
            val serverFile = File(config.mcpServerPath)
            if (!serverFile.exists() || !serverFile.canExecute()) {
                logger.warn("MCP server not found or not executable: ${config.mcpServerPath}")
                return false
            }
            
            // Start MCP server process
            mcpProcess = ProcessBuilder(serverFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            isConnected = true
            logger.info("MCP server connected successfully")
            true
        } catch (e: Exception) {
            logger.error("Failed to connect to MCP server", e)
            isConnected = false
            false
        }
    }
    
    /**
     * Disconnect from MCP server
     */
    fun disconnect() {
        try {
            mcpProcess?.destroy()
            mcpProcess = null
            isConnected = false
            logger.info("MCP server disconnected")
        } catch (e: Exception) {
            logger.error("Failed to disconnect from MCP server", e)
        }
    }
    
    /**
     * Get Figma screenshot using MCP
     */
    suspend fun getFigmaScreenshot(fileKey: String, nodeId: String): ByteArray? {
        return try {
            if (!isConnected) {
                logger.warn("MCP not connected, cannot get screenshot")
                return null
            }
            
            // Send MCP request for screenshot
            val request = createMcpRequest("get_screenshot", mapOf(
                "file_key" to fileKey,
                "node_id" to nodeId
            ))
            
            val response = sendMcpRequest(request)
            parseScreenshotResponse(response)
        } catch (e: Exception) {
            logger.error("Failed to get Figma screenshot via MCP", e)
            null
        }
    }
    
    /**
     * Get Figma node structure using MCP
     */
    suspend fun getFigmaNodeStructure(fileKey: String, nodeId: String): Map<String, Any>? {
        return try {
            if (!isConnected) {
                logger.warn("MCP not connected, cannot get node structure")
                return null
            }
            
            // Send MCP request for node structure
            val request = createMcpRequest("get_node_structure", mapOf(
                "file_key" to fileKey,
                "node_id" to nodeId
            ))
            
            val response = sendMcpRequest(request)
            parseNodeStructureResponse(response)
        } catch (e: Exception) {
            logger.error("Failed to get Figma node structure via MCP", e)
            null
        }
    }
    
    /**
     * Get Figma design tokens using MCP
     */
    suspend fun getFigmaDesignTokens(fileKey: String): Map<String, Any>? {
        return try {
            if (!isConnected) {
                logger.warn("MCP not connected, cannot get design tokens")
                return null
            }
            
            // Send MCP request for design tokens
            val request = createMcpRequest("get_variable_defs", mapOf(
                "file_key" to fileKey
            ))
            
            val response = sendMcpRequest(request)
            parseDesignTokensResponse(response)
        } catch (e: Exception) {
            logger.error("Failed to get Figma design tokens via MCP", e)
            null
        }
    }
    
    private suspend fun testMcpConnection(serverPath: String): Boolean {
        return try {
            val serverFile = File(serverPath)
            if (!serverFile.exists() || !serverFile.canExecute()) {
                return false
            }
            
            // Try to start and immediately stop the server to test if it's working
            val testProcess = ProcessBuilder(serverFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            // Give it a moment to start
            kotlinx.coroutines.delay(1000)
            
            val isAlive = testProcess.isAlive
            testProcess.destroy()
            
            isAlive
        } catch (e: Exception) {
            logger.warn("MCP connection test failed", e)
            false
        }
    }
    
    private fun createMcpRequest(method: String, params: Map<String, String>): String {
        return """
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "$method",
            "params": {
                ${params.entries.joinToString(",") { "\"${it.key}\": \"${it.value}\"" }}
            }
        }
        """.trimIndent()
    }
    
    private suspend fun sendMcpRequest(request: String): String {
        val process = mcpProcess ?: throw IllegalStateException("MCP not connected")
        
        return try {
            // Send request to MCP server
            val writer = OutputStreamWriter(process.outputStream)
            writer.write(request)
            writer.write("\n")
            writer.flush()
            
            // Read response
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() ?: ""
        } catch (e: Exception) {
            logger.error("Failed to send MCP request", e)
            throw e
        }
    }
    
    private fun parseScreenshotResponse(response: String): ByteArray? {
        // TODO: Parse MCP response and extract image data
        // This would depend on the specific MCP server implementation
        return null
    }
    
    private fun parseNodeStructureResponse(response: String): Map<String, Any>? {
        // TODO: Parse MCP response and extract node structure
        // This would depend on the specific MCP server implementation
        return null
    }
    
    private fun parseDesignTokensResponse(response: String): Map<String, Any>? {
        // TODO: Parse MCP response and extract design tokens
        // This would depend on the specific MCP server implementation
        return null
    }
    
    private fun getCurrentConfiguration(): ForgeConfiguration {
        val forgeService = project.getService(ForgeService::class.java)
        return forgeService.getConfiguration()
    }
    
    fun getServiceScope(): CoroutineScope = serviceScope
    
    fun dispose() {
        disconnect()
        serviceScope.cancel()
        logger.info("MCP Service disposed")
    }
}
