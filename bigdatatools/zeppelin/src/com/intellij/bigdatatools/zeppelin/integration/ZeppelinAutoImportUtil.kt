package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinDefaultMarkers
import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinFileViewProvider
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.startOffset

object ZeppelinAutoImportUtil {
  fun findCurrentCell(psiElement: PsiElement): PsiCell? {
    val file = psiElement.containingFile
    return findCell(file, psiElement.textRange.startOffset)
  }

  fun findFirstCell(psiElement: PsiElement) = findFirstCell(psiElement.containingFile, psiElement.language)

  fun startsWithScala(viewProvider: ZeppelinFileViewProvider): Boolean = checkFirstCell(viewProvider, "scala")

  fun startsWithPython(viewProvider: ZeppelinFileViewProvider): Boolean = checkFirstCell(viewProvider, "python")

  private fun checkFirstCell(viewProvider: ZeppelinFileViewProvider, lang: String): Boolean =
    getFirstCell(viewProvider)?.let { cell ->
      ZeppelinDefaultMarkers.markers[cell.marker.removePrefix("%")] == lang
    } ?: false


  private fun getFirstCell(viewProvider: ZeppelinFileViewProvider): ZeppelinCell? =
    (viewProvider.virtualFile as? NotebookVirtualFile)?.notebook?.cells?.get(0) as? ZeppelinCell

  private fun findFirstCell(file: PsiFile, language: Language): PsiCell? {
    var c = file.firstChild
    while (c != null && c.language != language) c = c.nextSibling
    val offset = c?.textRange?.startOffset ?: return null

    return findCell(file, offset)
  }

  fun findSourceStart(psiCell: PsiCell, language: Language): PsiElement? {
    val psiFile = psiCell.containingFile
    val file = psiFile.virtualFile as NotebookVirtualFile
    val noteCell = file.notebook.getCellByOffset(psiCell.startOffset) ?: return null

    return psiFile.viewProvider.findElementAt(noteCell.textOffset, language)
  }

  fun findCell(file: PsiFile?, offset: Int): PsiCell? {
    file ?: return null
    return file.viewProvider.findElementAt(offset, file.viewProvider.baseLanguage)?.parent?.parent as? PsiCell
  }
}