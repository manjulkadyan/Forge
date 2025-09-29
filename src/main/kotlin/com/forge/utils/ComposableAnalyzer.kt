package com.forge.utils

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Utility class for analyzing Kotlin Composable functions
 * 
 * Provides methods to:
 * - Detect @Composable @Preview functions at cursor position
 * - Extract function metadata and parameters
 * - Validate Composable structure
 */
object ComposableAnalyzer {
    
    private val logger = thisLogger()
    
    /**
     * Information about a Composable function found at the cursor position
     */
    data class ComposableInfo(
        val functionName: String,
        val className: String?,
        val packageName: String?,
        val isPreview: Boolean,
        val hasParameters: Boolean,
        val lineNumber: Int,
        val columnNumber: Int,
        val fullQualifiedName: String
    )
    
    /**
     * Check if there's a valid @Composable @Preview function at the current cursor position
     */
    fun hasComposableAtCaret(editor: Editor, psiFile: PsiFile): Boolean {
        return try {
            val caretOffset = editor.caretModel.offset
            val element = psiFile.findElementAt(caretOffset)
            val function = PsiTreeUtil.getParentOfType(element, KtFunction::class.java)
            
            function?.isComposablePreview() == true
        } catch (e: Exception) {
            logger.warn("Error checking for Composable at caret", e)
            false
        }
    }
    
    /**
     * Analyze the Composable function at the current cursor position
     */
    fun analyzeComposableAtCaret(editor: Editor, psiFile: PsiFile): ComposableInfo? {
        return try {
            val caretOffset = editor.caretModel.offset
            val element = psiFile.findElementAt(caretOffset)
            val function = PsiTreeUtil.getParentOfType(element, KtFunction::class.java)
            
            if (function?.isComposablePreview() == true) {
                createComposableInfo(function, psiFile)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Error analyzing Composable at caret", e)
            null
        }
    }
    
    /**
     * Find all @Composable @Preview functions in the given file
     */
    fun findAllComposablePreviews(psiFile: PsiFile): List<ComposableInfo> {
        return try {
            val functions = PsiTreeUtil.findChildrenOfType(psiFile, KtFunction::class.java)
            functions.filter { it.isComposablePreview() }
                .map { createComposableInfo(it, psiFile) }
        } catch (e: Exception) {
            logger.warn("Error finding all Composable previews", e)
            emptyList()
        }
    }
    
    private fun KtFunction.isComposablePreview(): Boolean {
        return hasAnnotation("Composable") && hasAnnotation("Preview")
    }
    
    private fun KtFunction.hasAnnotation(annotationName: String): Boolean {
        return annotationEntries.any { annotation ->
            val name = annotation.calleeExpression?.text
            name == annotationName || name?.endsWith(".$annotationName") == true
        }
    }
    
    private fun createComposableInfo(function: KtFunction, psiFile: PsiFile): ComposableInfo {
        val containingClass = PsiTreeUtil.getParentOfType(function, KtClassOrObject::class.java)
        val packageName = (psiFile as? KtFile)?.packageFqName?.asString()
        
        val fullQualifiedName = buildString {
            packageName?.let { append("$it.") }
            containingClass?.name?.let { append("$it.") }
            append(function.name ?: "Unknown")
        }
        
        return ComposableInfo(
            functionName = function.name ?: "Unknown",
            className = containingClass?.name,
            packageName = packageName,
            isPreview = function.hasAnnotation("Preview"),
            hasParameters = function.valueParameters.isNotEmpty(),
            lineNumber = psiFile.getLineNumber(function.textOffset) + 1,
            columnNumber = function.textOffset - psiFile.getLineStartOffset(psiFile.getLineNumber(function.textOffset)) + 1,
            fullQualifiedName = fullQualifiedName
        )
    }
    
    private fun PsiFile.getLineNumber(offset: Int): Int {
        return com.intellij.openapi.util.TextRange(0, textLength).startOffset.let { startOffset ->
            text.substring(startOffset, offset).count { it == '\n' }
        }
    }
    
    private fun PsiFile.getLineStartOffset(lineNumber: Int): Int {
        var currentLine = 0
        var offset = 0
        for (char in text) {
            if (currentLine == lineNumber) break
            if (char == '\n') currentLine++
            offset++
        }
        return offset
    }
}
