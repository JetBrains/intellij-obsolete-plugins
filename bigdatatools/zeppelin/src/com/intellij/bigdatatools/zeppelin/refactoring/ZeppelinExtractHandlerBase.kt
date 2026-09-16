package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil

abstract class ZeppelinExtractHandlerBase : RefactoringActionHandler {
  companion object {
    fun guessSelection(file: ScalaFile, editor: Editor): List<PsiElement> {
      val scalaElements = ScalaRefactoringUtil.selectedElements(editor, file, false).iterator()
      val result = mutableListOf<PsiElement>()

      @Suppress("KotlinConstantConditions")
      while (scalaElements.hasNext()) {
        val e = scalaElements.next() as PsiElement
        if (e !is PsiWhiteSpace) result.add(e)
      }

      if (result.isEmpty()) {
        val asPsiFile = file as PsiFile
        val adjusted = TargetElementUtilBase.adjustOffset(asPsiFile, editor.document, editor.caretModel.offset)
        val element = asPsiFile.findElementAt(adjusted)

        return if (element == null || element is PsiWhiteSpace) emptyList() else listOf(element)
      }

      return result
    }
  }

  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
  }

  protected fun extractInfoInner(editor: Editor?, file: PsiFile?): ZeppelinExtractRefactoringUtil.ExtractedJobInfo? {
    if (editor == null || file == null) return null

    extractScalaFile(file)?.let { scalaFile ->
      val elements = extractRefactoringElements(scalaFile as ScalaFile, editor)
      if (elements.isEmpty()) return null

      return ZeppelinExtractRefactoringUtil.extractJobInfo(scalaFile, elements)
    }

    return null
  }

  private fun extractScalaFile(el: PsiElement?) = el?.containingFile?.viewProvider?.getPsi(ScalaLanguage.INSTANCE)

  protected open fun extractRefactoringElements(file: ScalaFile, editor: Editor): List<PsiElement> = guessSelection(file, editor)
}