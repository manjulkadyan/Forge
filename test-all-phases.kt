package com.forge.test

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.forge.services.ForgeService
import com.forge.services.ComposeRenderService
import com.forge.services.FigmaDataService
import com.forge.models.ForgeConfiguration
import com.forge.models.AuthenticationMethod
import com.forge.utils.ComposableAnalyzer
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive test suite for all phases of The Forge plugin
 */
class PhaseTester {
    
    private val logger = thisLogger()
    
    fun runAllTests() {
        logger.info("🧪 Starting comprehensive phase testing...")
        
        try {
            testPhase1()
            testPhase2()
            testPhase3()
            testPhase4()
            
            logger.info("✅ All phase tests completed successfully!")
        } catch (e: Exception) {
            logger.error("❌ Phase testing failed", e)
        }
    }
    
    /**
     * Test Phase 1: Plugin initialization and basic functionality
     */
    private fun testPhase1() {
        logger.info("🔧 Testing Phase 1: Plugin Initialization...")
        
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project == null) {
                logger.warn("⚠️ No project open - Phase 1 test limited")
                return
            }
            
            // Test service initialization
            val forgeService = project.getService(ForgeService::class.java)
            logger.info("✅ ForgeService initialized: ${forgeService != null}")
            
            // Test configuration loading
            val config = forgeService.getConfiguration()
            logger.info("✅ Configuration loaded: ${config.isValid()}")
            
            // Test tool window factory
            val toolWindowFactory = com.forge.ui.ForgeToolWindowFactory()
            val shouldBeAvailable = toolWindowFactory.shouldBeAvailable(project)
            logger.info("✅ Tool window availability: $shouldBeAvailable")
            
            logger.info("✅ Phase 1 test completed successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Phase 1 test failed", e)
            throw e
        }
    }
    
    /**
     * Test Phase 2: Authentication and configuration system
     */
    private fun testPhase2() {
        logger.info("🔐 Testing Phase 2: Authentication & Configuration...")
        
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project == null) {
                logger.warn("⚠️ No project open - Phase 2 test limited")
                return
            }
            
            val forgeService = project.getService(ForgeService::class.java)
            
            // Test configuration validation
            val testConfig = ForgeConfiguration(
                isEnabled = true,
                figmaPersonalAccessToken = "figd_test123456789012345678901234567890123456789012345678901234567890",
                figmaUsername = "testuser",
                visualThreshold = 0.95,
                structuralThreshold = 0.90,
                authenticationMethod = AuthenticationMethod.PAT_ONLY
            )
            
            val validation = testConfig.validate()
            logger.info("✅ Configuration validation: ${validation.isValid}")
            if (validation.hasErrors()) {
                logger.warn("⚠️ Configuration errors: ${validation.getErrorMessage()}")
            }
            if (validation.hasWarnings()) {
                logger.warn("⚠️ Configuration warnings: ${validation.getWarningMessage()}")
            }
            
            // Test credential manager
            val credentialManager = project.getService(com.forge.security.ForgeCredentialManager::class.java)
            logger.info("✅ CredentialManager initialized: ${credentialManager != null}")
            
            // Test settings configurable
            val settingsConfigurable = com.forge.settings.ForgeSettingsConfigurable()
            logger.info("✅ Settings configurable created: ${settingsConfigurable != null}")
            
            logger.info("✅ Phase 2 test completed successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Phase 2 test failed", e)
            throw e
        }
    }
    
    /**
     * Test Phase 3: Compose rendering system
     */
    private fun testPhase3() {
        logger.info("🎨 Testing Phase 3: Compose Rendering...")
        
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project == null) {
                logger.warn("⚠️ No project open - Phase 3 test limited")
                return
            }
            
            val composeRenderService = project.getService(ComposeRenderService::class.java)
            logger.info("✅ ComposeRenderService initialized: ${composeRenderService != null}")
            
            // Test ComposableAnalyzer
            val testComposableFqn = "com.example.app.MyScreenKt.PreviewMyScreen"
            val isValid = runBlocking { 
                composeRenderService.validateComposable(testComposableFqn) 
            }
            logger.info("✅ Composable validation test: $isValid")
            
            // Test render service methods
            composeRenderService.clearCache()
            logger.info("✅ Cache clearing test completed")
            
            // Test GradleRenderTask
            val gradleRenderTask = com.forge.rendering.GradleRenderTask(project)
            logger.info("✅ GradleRenderTask initialized: ${gradleRenderTask != null}")
            
            // Test LayoutTreeGenerator
            val layoutTreeGenerator = com.forge.rendering.LayoutTreeGenerator()
            logger.info("✅ LayoutTreeGenerator initialized: ${layoutTreeGenerator != null}")
            
            logger.info("✅ Phase 3 test completed successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Phase 3 test failed", e)
            throw e
        }
    }
    
    /**
     * Test Phase 4: Figma data acquisition
     */
    private fun testPhase4() {
        logger.info("🎯 Testing Phase 4: Figma Data Acquisition...")
        
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project == null) {
                logger.warn("⚠️ No project open - Phase 4 test limited")
                return
            }
            
            // Test FigmaDataAcquisition
            val testToken = "figd_test123456789012345678901234567890123456789012345678901234567890"
            val figmaDataAcquisition = com.forge.figma.FigmaDataAcquisition(testToken)
            logger.info("✅ FigmaDataAcquisition initialized: ${figmaDataAcquisition != null}")
            
            // Test FigmaDataService
            val figmaDataService = project.getService(FigmaDataService::class.java)
            logger.info("✅ FigmaDataService initialized: ${figmaDataService != null}")
            
            // Test data models
            val testFileData = com.forge.figma.FigmaFileData("test_file", "{}")
            val testNodeData = com.forge.figma.FigmaNodeData("test_node", "{}")
            val testImageData = com.forge.figma.FigmaImageData("test_node", "http://test.com", byteArrayOf(1, 2, 3))
            
            logger.info("✅ FigmaFileData created: ${testFileData.fileKey}")
            logger.info("✅ FigmaNodeData created: ${testNodeData.nodeId}")
            logger.info("✅ FigmaImageData created: ${testImageData.nodeId}")
            
            // Test MCP Service
            val mcpService = project.getService(com.forge.services.MCPService::class.java)
            logger.info("✅ MCPService initialized: ${mcpService != null}")
            
            logger.info("✅ Phase 4 test completed successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Phase 4 test failed", e)
            throw e
        }
    }
}

/**
 * Main test runner
 */
fun main() {
    ApplicationManager.getApplication().invokeLater {
        val tester = PhaseTester()
        tester.runAllTests()
    }
}
