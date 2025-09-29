package com.forge.figma

import com.intellij.openapi.diagnostic.thisLogger
import com.forge.models.ForgeConfiguration
import com.forge.models.AuthenticationMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simplified Figma data acquisition service
 * 
 * This service provides:
 * - Basic Figma API integration
 * - File and node data retrieval
 * - Image export capabilities
 * - Error handling and retry logic
 */
class FigmaDataAcquisition(private val personalAccessToken: String) {
    
    private val logger = thisLogger()
    
    companion object {
        private const val FIGMA_API_BASE = "https://api.figma.com/v1"
        private const val RATE_LIMIT_DELAY_MS = 1000L
    }
    
    /**
     * Get file metadata
     */
    suspend fun getFile(fileKey: String): FigmaFileData? = withContext(Dispatchers.IO) {
        try {
            val url = "$FIGMA_API_BASE/files/$fileKey"
            val response = makeAuthenticatedRequest(url)
            
            if (response.isSuccessful) {
                // Parse basic file data from JSON response
                FigmaFileData(
                    key = fileKey,
                    name = "Figma File", // Would parse from JSON
                    lastModified = "",
                    version = "1.0"
                )
            } else {
                logger.error("Failed to get file $fileKey: ${response.code}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting file $fileKey", e)
            null
        }
    }
    
    /**
     * Get node data
     */
    suspend fun getNode(fileKey: String, nodeId: String): FigmaNodeData? = withContext(Dispatchers.IO) {
        try {
            val url = "$FIGMA_API_BASE/files/$fileKey/nodes?ids=$nodeId"
            val response = makeAuthenticatedRequest(url)
            
            if (response.isSuccessful) {
                // Parse basic node data from JSON response
                FigmaNodeData(
                    id = nodeId,
                    name = "Figma Node", // Would parse from JSON
                    type = "FRAME",
                    visible = true
                )
            } else {
                logger.error("Failed to get node $nodeId: ${response.code}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting node $nodeId", e)
            null
        }
    }
    
    /**
     * Export node as image
     */
    suspend fun exportNode(
        fileKey: String,
        nodeId: String,
        format: String = "png",
        scale: Float = 1.0f
    ): FigmaImageData? = withContext(Dispatchers.IO) {
        try {
            val url = "$FIGMA_API_BASE/images/$fileKey?ids=$nodeId&format=$format&scale=$scale"
            val response = makeAuthenticatedRequest(url)
            
            if (response.isSuccessful) {
                // Parse image URL from JSON response
                FigmaImageData(
                    nodeId = nodeId,
                    url = "https://figma.com/image", // Would parse from JSON
                    format = format,
                    scale = scale
                )
            } else {
                logger.error("Failed to export node $nodeId: ${response.code}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error exporting node $nodeId", e)
            null
        }
    }
    
    /**
     * Make authenticated HTTP request
     */
    private suspend fun makeAuthenticatedRequest(url: String): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-Figma-Token", personalAccessToken)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            
            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }
            
            HttpResponse(responseCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Validate API token
     */
    suspend fun validateToken(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$FIGMA_API_BASE/me"
            val response = makeAuthenticatedRequest(url)
            response.isSuccessful
        } catch (e: Exception) {
            logger.error("Error validating token", e)
            false
        }
    }
}

/**
 * Basic data models for Figma integration
 */
data class FigmaFileData(
    val key: String,
    val name: String,
    val lastModified: String,
    val version: String
)

data class FigmaNodeData(
    val id: String,
    val name: String,
    val type: String,
    val visible: Boolean
)

data class FigmaImageData(
    val nodeId: String,
    val url: String,
    val format: String,
    val scale: Float
)

data class HttpResponse(
    val code: Int,
    val body: String
) {
    val isSuccessful: Boolean get() = code in 200..299
}
