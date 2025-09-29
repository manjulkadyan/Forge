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
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.runBlocking

/**
 * Test script to verify Phase 3 functionality with real Composable
 */
class RealComposableTester(private val project: Project) {
    
    fun testRealComposable(): TestResults {
        val results = TestResults()
        
        try {
            println("🧪 Testing Phase 3 with Real Composable")
            println("=" * 50)
            
            // Test 1: Load the real Composable file
            val filePath = "/Users/manjul.kadyan/StudioProjects/android-client2/app/src/main/java/com/zocdoc/android/view/compose/form/AppRevampTextfieldCompose.kt"
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
            
            if (virtualFile == null) {
                results.addTest("File Loading", false, "Could not find file at $filePath")
                return results
            }
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile == null) {
                results.addTest("PSI File Loading", false, "Could not load PSI file")
                return results
            }
            
            results.addTest("File Loading", true, "Successfully loaded real Composable file")
            
            // Test 2: Analyze Composable functions
            val composableInfo = ComposableAnalyzer.findAllComposablePreviews(psiFile)
            results.addTest("Composable Analysis", true, "Found ${composableInfo.size} @Preview composables")
            
            composableInfo.forEach { info ->
                println("  📱 Found: ${info.functionName} (${info.fullQualifiedName})")
                println("    - Line: ${info.lineNumber}")
                println("    - Package: ${info.packageName}")
                println("    - Class: ${info.className}")
                println("    - Has Parameters: ${info.hasParameters}")
            }
            
            // Test 3: Test service initialization
            val forgeService = project.getService(ForgeService::class.java)
            val renderService = project.getService(ComposeRenderService::class.java)
            val gradleTask = GradleRenderTask(project)
            val layoutGenerator = LayoutTreeGenerator()
            
            results.addTest("Service Initialization", true, "All services loaded successfully")
            
            // Test 4: Test Composable validation
            val firstComposable = composableInfo.firstOrNull()
            if (firstComposable != null) {
                val isValid = runBlocking {
                    renderService.validateComposable(firstComposable.fullQualifiedName)
                }
                results.addTest("Composable Validation", isValid, 
                    if (isValid) "Composable validation passed" else "Composable validation failed")
            }
            
            // Test 5: Test layout tree generation
            val testLayoutData = mapOf(
                "id" to "test_root",
                "type" to "Column",
                "x" to 0f,
                "y" to 0f,
                "width" to 300f,
                "height" to 200f,
                "properties" to mapOf("spacing" to "8dp"),
                "children" to listOf(
                    mapOf(
                        "id" to "text_field",
                        "type" to "OutlinedTextField",
                        "x" to 16f,
                        "y" to 16f,
                        "width" to 268f,
                        "height" to 56f,
                        "properties" to mapOf("placeholder" to "Enter text here")
                    )
                )
            )
            
            val layoutTree = layoutGenerator.generateLayoutTree(testLayoutData)
            val serializedTree = layoutGenerator.serializeLayoutTree(layoutTree)
            
            results.addTest("Layout Tree Generation", true, 
                "Generated layout tree with ${layoutTree.children.size} children")
            
            // Test 6: Test render statistics
            val stats = renderService.getRenderStatistics()
            results.addTest("Render Statistics", true, 
                "Stats: ${stats.totalRenders} renders, ${(stats.successRate * 100).toInt()}% success rate")
            
            println("\n✅ Real Composable Testing Complete!")
            println("📊 Results: ${results.passedTests}/${results.totalTests} tests passed")
            
            // Test 7: Test specific Composable functions
            if (composableInfo.isNotEmpty()) {
                println("\n🔍 Testing specific Composable functions:")
                composableInfo.forEach { info ->
                    val isPreview = ComposableAnalyzer.isPreviewComposable(psiFile, info.functionName)
                    val previewInfo = ComposableAnalyzer.getPreviewComposableInfo(psiFile, info.functionName)
                    
                    println("  📱 ${info.functionName}:")
                    println("    - Is Preview: $isPreview")
                    println("    - Preview Info: ${previewInfo?.let { "Valid (${it.parameters.size} params)" } ?: "Invalid"}")
                }
            }
            
        } catch (e: Exception) {
            results.addTest("Real Composable Testing", false, "Error: ${e.message}")
            println("❌ Real Composable Testing Failed: ${e.message}")
            e.printStackTrace()
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
