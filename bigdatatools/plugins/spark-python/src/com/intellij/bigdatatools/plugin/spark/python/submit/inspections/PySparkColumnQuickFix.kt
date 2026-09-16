package com.intellij.bigdatatools.plugin.spark.python.submit.inspections

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class PySparkColumnQuickFix : LocalQuickFix {
  override fun getFamilyName(): String = SparkMessagesBundle.message("replace.with.allowed.value")

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val initialElement = descriptor.psiElement
    val internalNode = PsiTreeUtil.findChildOfType(initialElement, PyStringLiteralExpression::class.java, false) ?: return

    initialElement.containingFile.originalFile.virtualFile
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(
      initialElement.containingFile.originalFile.virtualFile) as? TextEditor
                     ?: return
    val editor = fileEditor.editor
    invokeLater {
      val child = internalNode.firstChild ?: return@invokeLater
      editor.selectionModel.setSelection(child.startOffset + 1, child.endOffset - 1)
      CodeCompletionHandlerBase.createHandler(CompletionType.BASIC).invokeCompletion(project, editor)
    }
  }
}