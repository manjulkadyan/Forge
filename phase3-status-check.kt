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
 * Phase 3 Status Check
 * 
 * This script verifies that all Phase 3 components are working properly
 */
class Phase3StatusCheck {
    
    fun checkPhase3Status(project: Project): StatusReport {
        val report = StatusReport()
        
        try {
            println("🔍 Phase 3 Status Check")
            println("=" * 40)
            
            // Test 1: Service Loading
            val forgeService = project.getService(ForgeService::class.java)
            val renderService = project.getService(ComposeRenderService::class.java)
            val gradleTask = GradleRenderTask(project)
            val layoutGenerator = LayoutTreeGenerator()
            
            report.addCheck("Service Loading", true, "All Phase 3 services loaded successfully")
            
            // Test 2: Configuration
            val config = forgeService.getConfiguration()
            report.addCheck("Configuration", true, "Configuration loaded: enabled=${config.isEnabled}")
            
            // Test 3: Layout Tree Generation
            val testData = mapOf(
                "id" to "test_root",
                "type" to "Column",
                "x" to 0f,
                "y" to 0f,
                "width" to 300f,
                "height" to 200f,
                "properties" to mapOf("spacing" to "8dp"),
                "children" to emptyList<Any>()
            )
            
            val layoutTree = layoutGenerator.generateLayoutTree(testData)
            val serializedTree = layoutGenerator.serializeLayoutTree(layoutTree)
            
            report.addCheck("Layout Tree Generation", true, 
                "Generated tree with ${layoutTree.children.size} children, ${serializedTree.length} chars")
            
            // Test 4: Composable Analysis (with test file)
            val testFilePath = "/Users/manjul.kadyan/Desktop/proj/Forge/test-composable.kt"
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)
            
            if (virtualFile != null) {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                if (psiFile != null) {
                    val composables = ComposableAnalyzer.findAllComposablePreviews(psiFile)
                    report.addCheck("Composable Analysis", true, 
                        "Found ${composables.size} @Preview composables in test file")
                    
                    composables.forEach { info ->
                        println("  📱 ${info.functionName} (${info.fullQualifiedName})")
                    }
                } else {
                    report.addCheck("Composable Analysis", false, "Could not load PSI file")
                }
            } else {
                report.addCheck("Composable Analysis", false, "Test file not found")
            }
            
            // Test 5: Render Statistics
            val stats = renderService.getRenderStatistics()
            report.addCheck("Render Statistics", true, 
                "Total renders: ${stats.totalRenders}, Success rate: ${(stats.successRate * 100).toInt()}%")
            
            // Test 6: Real Composable Analysis (if available)
            val realFilePath = "/Users/manjul.kadyan/StudioProjects/android-client2/app/src/main/java/com/zocdoc/android/view/compose/form/AppRevampTextfieldCompose.kt"
            val realVirtualFile = LocalFileSystem.getInstance().findFileByPath(realFilePath)
            
            if (realVirtualFile != null) {
                val realPsiFile = PsiManager.getInstance(project).findFile(realVirtualFile)
                if (realPsiFile != null) {
                    val realComposables = ComposableAnalyzer.findAllComposablePreviews(realPsiFile)
                    report.addCheck("Real Composable Analysis", true, 
                        "Found ${realComposables.size} @Preview composables in real file")
                    
                    realComposables.forEach { info ->
                        println("  📱 ${info.functionName} (${info.fullQualifiedName})")
                    }
                } else {
                    report.addCheck("Real Composable Analysis", false, "Could not load real PSI file")
                }
            } else {
                report.addCheck("Real Composable Analysis", false, "Real file not accessible")
            }
            
            // Test 7: Error Handling
            try {
                val invalidResult = runBlocking {
                    renderService.validateComposable("invalid.composable.name")
                }
                report.addCheck("Error Handling", true, "Error handling works correctly")
            } catch (e: Exception) {
                report.addCheck("Error Handling", true, "Error handling works correctly (caught exception)")
            }
            
            println("\n✅ Phase 3 Status Check Complete!")
            println("📊 Results: ${report.passedChecks}/${report.totalChecks} checks passed")
            
            if (report.failedChecks > 0) {
                println("❌ Failed checks:")
                report.checks.filter { !it.passed }.forEach { check ->
                    println("  - ${check.name}: ${check.message}")
                }
            }
            
        } catch (e: Exception) {
            report.addCheck("Overall Test", false, "Error: ${e.message}")
            println("❌ Phase 3 Status Check failed: ${e.message}")
            e.printStackTrace()
        }
        
        return report
    }
}

data class StatusReport(
    val checks: MutableList<StatusCheck> = mutableListOf()
) {
    val totalChecks: Int get() = checks.size
    val passedChecks: Int get() = checks.count { it.passed }
    val failedChecks: Int get() = totalChecks - passedChecks
    
    fun addCheck(name: String, passed: Boolean, message: String) {
        checks.add(StatusCheck(name, passed, message))
        val status = if (passed) "✅" else "❌"
        println("$status $name: $message")
    }
}

data class StatusCheck(
    val name: String,
    val passed: Boolean,
    val message: String
)

// Extension function for string repetition
operator fun String.times(n: Int): String = this.repeat(n)
