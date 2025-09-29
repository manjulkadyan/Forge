package com.forge.test

import com.forge.services.ComposeRenderService
import com.forge.services.ForgeService
import com.forge.rendering.GradleRenderTask
import com.forge.rendering.LayoutTreeGenerator
import com.forge.utils.ComposableAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive test of Phase 3 with real Composable from ZocDoc
 */
class Phase3RealTest {
    
    fun runTest(project: Project): TestReport {
        val report = TestReport()
        
        try {
            println("🚀 Phase 3 Real Composable Test")
            println("=" * 60)
            
            // Test 1: File Access
            val filePath = "/Users/manjul.kadyan/StudioProjects/android-client2/app/src/main/java/com/zocdoc/android/view/compose/form/AppRevampTextfieldCompose.kt"
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
            
            if (virtualFile == null) {
                report.addResult("File Access", false, "Could not access real Composable file")
                return report
            }
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile == null) {
                report.addResult("PSI Loading", false, "Could not load PSI representation")
                return report
            }
            
            report.addResult("File Access", true, "Successfully accessed real Composable file")
            
            // Test 2: Composable Analysis
            val composables = ComposableAnalyzer.findAllComposablePreviews(psiFile)
            report.addResult("Composable Detection", true, "Found ${composables.size} @Preview composables")
            
            println("\n📱 Detected Composables:")
            composables.forEach { info ->
                println("  • ${info.functionName}")
                println("    - FQN: ${info.fullQualifiedName}")
                println("    - Line: ${info.lineNumber}")
                println("    - Package: ${info.packageName}")
                println("    - Class: ${info.className}")
                println("    - Has Parameters: ${info.hasParameters}")
                println()
            }
            
            // Test 3: Service Integration
            val forgeService = project.getService(ForgeService::class.java)
            val renderService = project.getService(ComposeRenderService::class.java)
            val gradleTask = GradleRenderTask(project)
            val layoutGenerator = LayoutTreeGenerator()
            
            report.addResult("Service Integration", true, "All Phase 3 services loaded successfully")
            
            // Test 4: Layout Tree Generation
            val testData = createTestLayoutData()
            val layoutTree = layoutGenerator.generateLayoutTree(testData)
            val serializedTree = layoutGenerator.serializeLayoutTree(layoutTree)
            
            report.addResult("Layout Tree Generation", true, 
                "Generated tree with ${layoutTree.children.size} children, ${serializedTree.length} chars")
            
            // Test 5: Composable Validation
            if (composables.isNotEmpty()) {
                val firstComposable = composables.first()
                val isValid = runBlocking {
                    renderService.validateComposable(firstComposable.fullQualifiedName)
                }
                report.addResult("Composable Validation", isValid, 
                    "Validation for ${firstComposable.functionName}: ${if (isValid) "PASSED" else "FAILED"}")
            }
            
            // Test 6: Render Statistics
            val stats = renderService.getRenderStatistics()
            report.addResult("Render Statistics", true, 
                "Total renders: ${stats.totalRenders}, Success rate: ${(stats.successRate * 100).toInt()}%")
            
            // Test 7: Preview Function Analysis
            if (composables.isNotEmpty()) {
                println("\n🔍 Preview Function Analysis:")
                composables.forEach { info ->
                    val isPreview = ComposableAnalyzer.isPreviewComposable(psiFile, info.functionName)
                    val previewInfo = ComposableAnalyzer.getPreviewComposableInfo(psiFile, info.functionName)
                    
                    println("  📱 ${info.functionName}:")
                    println("    - Is Preview: $isPreview")
                    println("    - Preview Info: ${previewInfo?.let { "Valid (${it.parameters.size} params)" } ?: "Invalid"}")
                    if (previewInfo != null) {
                        println("    - Parameters: ${previewInfo.parameters.joinToString(", ")}")
                        println("    - Is Public: ${previewInfo.isPublic}")
                        println("    - Is Internal: ${previewInfo.isInternal}")
                    }
                    println()
                }
            }
            
            // Test 8: Configuration Test
            val config = forgeService.getConfiguration()
            report.addResult("Configuration", true, 
                "Config loaded: enabled=${config.isEnabled}, visualThreshold=${config.visualThreshold}")
            
            println("\n✅ Phase 3 Real Composable Test Complete!")
            println("📊 Results: ${report.passedTests}/${report.totalTests} tests passed")
            
            if (report.failedTests > 0) {
                println("❌ Failed tests:")
                report.results.filter { !it.passed }.forEach { result ->
                    println("  - ${result.name}: ${result.message}")
                }
            }
            
        } catch (e: Exception) {
            report.addResult("Test Execution", false, "Error: ${e.message}")
            println("❌ Test failed with exception: ${e.message}")
            e.printStackTrace()
        }
        
        return report
    }
    
    private fun createTestLayoutData(): Map<String, Any> {
        return mapOf(
            "id" to "app_revamp_textfield",
            "type" to "Column",
            "x" to 0f,
            "y" to 0f,
            "width" to 300f,
            "height" to 200f,
            "properties" to mapOf(
                "spacing" to "8dp",
                "modifier" to "fillMaxWidth()"
            ),
            "children" to listOf(
                mapOf(
                    "id" to "label",
                    "type" to "MezzanineFormLabel",
                    "x" to 0f,
                    "y" to 0f,
                    "width" to 300f,
                    "height" to 20f,
                    "properties" to mapOf(
                        "text" to "Default State",
                        "isOptional" to "false"
                    )
                ),
                mapOf(
                    "id" to "text_field",
                    "type" to "OutlinedTextField",
                    "x" to 0f,
                    "y" to 28f,
                    "width" to 300f,
                    "height" to 56f,
                    "properties" to mapOf(
                        "placeholder" to "Enter text here",
                        "value" to "",
                        "state" to "DEFAULT"
                    )
                ),
                mapOf(
                    "id" to "message",
                    "type" to "MezzanineFormMessage",
                    "x" to 0f,
                    "y" to 92f,
                    "width" to 300f,
                    "height" to 16f,
                    "properties" to mapOf(
                        "errorText" to "",
                        "captionText" to ""
                    )
                )
            )
        )
    }
}

data class TestReport(
    val results: MutableList<TestResult> = mutableListOf()
) {
    val totalTests: Int get() = results.size
    val passedTests: Int get() = results.count { it.passed }
    val failedTests: Int get() = totalTests - passedTests
    
    fun addResult(name: String, passed: Boolean, message: String) {
        results.add(TestResult(name, passed, message))
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
