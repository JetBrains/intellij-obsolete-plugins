package com.intellij.bigdatatools.notebooks.core.api.psi

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

interface NotebookASTCellMapper {
  fun isSupported(virtualFile: NotebookVirtualFile): Boolean

  fun createNoteCell(virtualFile: NotebookVirtualFile,
                     psiCell: PsiCell): NotebookCell

  fun createNoteStemCell(virtualFile: NotebookVirtualFile,
                         psiStemCell: PsiStemCell,
                         mayDropLast: Boolean = true): NotebookCell

  fun updateNoteCellByMarker(virtualFile: NotebookVirtualFile,
                             cell: NotebookCell,
                             oldMarker: PsiElement,
                             newMarker: PsiElement,
                             mayDropLast: Boolean = true): NotebookVirtualFile?

  fun updateNoteCellSource(virtualFile: NotebookVirtualFile,
                           cell: NotebookCell,
                           psiCell: PsiCell,
                           mayDropLast: Boolean)

  companion object {
    private const val ID: String = "com.intellij.bigdatatools.corenotebook.bdtNotebookASTCellMapper"
    val EP_NAME: ExtensionPointName<NotebookASTCellMapper> = ExtensionPointName.create(ID)
  }
}