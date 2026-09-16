package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteEditor
import com.intellij.bigdatatools.notebooks.core.impl.editor.getPsiFile
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinTemplateTypes
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

internal abstract class ZeppelinExtractForActionHandler : ZeppelinExtractJobHandler(ZeppelinExtractJobAction()) {
  fun invokeFromContext(e: AnActionEvent) {
    val editor = e.noteEditor ?: return
    val psiFile = editor.getPsiFile() ?: return

    invoke(psiFile.project, editor, psiFile, e.dataContext)
  }

  protected fun findActualOffset(cell: PsiCell, file: PsiFile): PsiElement? {
    val fakeStart = file.findElementAt(cell.textOffset)?.textLength ?: return null
    return file.findElementAt(cell.textOffset + fakeStart + 1)?.let {
      ZeppelinExtractRefactoringUtil.getTopLevelElement(it) }
  }

  protected fun enumerateElements(file: ScalaFile, from: PsiElement, to: PsiElement?): List<PsiElement> {
    val result = mutableListOf<PsiElement>()
    var current: PsiElement? = from

    while (current != null && current != to) {
      if (current.language == (file as PsiFile).language && current.elementType != ZeppelinTemplateTypes.OUTER) result.add(current)
      current = current.nextSibling
    }

    return result
  }
}