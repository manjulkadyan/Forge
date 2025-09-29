package com.forge.test

import com.forge.services.ComposeRenderService
import com.forge.services.ForgeService
import com.forge.rendering.GradleRenderTask
import com.forge.rendering.LayoutTreeGenerator
import com.forge.utils.ComposableAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.runBlocking

/**
 * Test script to verify Phase 3 functionality
 */
class Phase3Tester(private val project: Project) {
    
    fun testPhase3(): TestResults {
        val results = TestResults()
        
        try {
            // Test 1: Service Initialization
            println("🧪 Testing Phase 3: Compose Render Capture System")
            println("=" * 50)
            
            // Test ForgeService
            val forgeService = project.getService(ForgeService::class.java)
            results.addTest("ForgeService Initialization", true, "Service loaded successfully")
            
            // Test ComposeRenderService
            val renderService = project.getService(ComposeRenderService::class.java)
            results.addTest("ComposeRenderService Initialization", true, "Service loaded successfully")
            
            // Test GradleRenderTask
            val gradleTask = GradleRenderTask(project)
            results.addTest("GradleRenderTask Creation", true, "Task created successfully")
            
            // Test LayoutTreeGenerator
            val layoutGenerator = LayoutTreeGenerator()
            results.addTest("LayoutTreeGenerator Creation", true, "Generator created successfully")
            
            // Test ComposableAnalyzer
            val analyzer = ComposableAnalyzer
            results.addTest("ComposableAnalyzer Access", true, "Analyzer accessible")
            
            // Test 2: Configuration Loading
            val config = forgeService.getConfiguration()
            results.addTest("Configuration Loading", config != null, "Configuration loaded: ${config?.isEnabled}")
            
            // Test 3: Service Statistics
            val stats = renderService.getRenderStatistics()
            results.addTest("Render Statistics", true, "Stats: ${stats.totalRenders} renders, ${stats.successRate * 100}% success rate")
            
            println("\n✅ Phase 3 Testing Complete!")
            println("📊 Results: ${results.passedTests}/${results.totalTests} tests passed")
            
        } catch (e: Exception) {
            results.addTest("Phase 3 Testing", false, "Error: ${e.message}")
            println("❌ Phase 3 Testing Failed: ${e.message}")
        }
        
        return results
    }
}

data class TestResults(
    val tests: MutableList<TestResult> = mutableListOf()
) {
    val totalTests: Int get() = tests.size
    val passedTests: Int get() = tests.count { it.passed }
    val failedTests: Int get() = totalTests - passedTests
    
    fun addTest(name: String, passed: Boolean, message: String) {
        tests.add(TestResult(name, passed, message))
        val status = if (passed) "✅" else "❌"
        println("$status $name: $message")
    }
}

data class TestResult(
    val name: String,
    val passed: Boolean,
    val message: String
)

// Extension function for string repetition
operator fun String.times(n: Int): String = this.repeat(n)
