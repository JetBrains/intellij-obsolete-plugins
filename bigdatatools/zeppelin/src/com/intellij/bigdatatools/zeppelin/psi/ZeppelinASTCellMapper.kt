// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.psi.NotebookASTCellMapper
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiStemCell
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.PsiCellBase
import com.intellij.bigdatatools.zeppelin.file.isNoteFile
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCellBuilder
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.psi.PsiElement


class ZeppelinASTCellMapper : NotebookASTCellMapper {
  override fun isSupported(virtualFile: NotebookVirtualFile) = virtualFile.isNoteFile

  override fun createNoteStemCell(virtualFile: NotebookVirtualFile,
                                  psiStemCell: PsiStemCell, mayDropLast: Boolean): NotebookCell =
    ZeppelinCellBuilder.createFromSource(virtualFile.notebook as ZeppelinNotebook, psiStemCell.source(true))

  override fun createNoteCell(virtualFile: NotebookVirtualFile, psiCell: PsiCell): NotebookCell {
    val cellText = psiCell.source(true)
    val zeppelinNotebook = virtualFile.notebook as ZeppelinNotebook
    return ZeppelinCellBuilder.createFromSource(zeppelinNotebook, cellText)
  }

  override fun updateNoteCellByMarker(virtualFile: NotebookVirtualFile,
                                      cell: NotebookCell,
                                      oldMarker: PsiElement,
                                      newMarker: PsiElement,
                                      mayDropLast: Boolean): NotebookVirtualFile {
    val psiCell = oldMarker.parent as PsiCell
    cell.source = psiCell.source(mayDropLast)
    return virtualFile
  }

  override fun updateNoteCellSource(virtualFile: NotebookVirtualFile,
                                    cell: NotebookCell,
                                    psiCell: PsiCell,
                                    mayDropLast: Boolean) {
    cell.source = psiCell.source(mayDropLast)
  }

  private fun PsiCellBase.source(mayDropLast: Boolean = true): String = text
    .run {
      if (mayDropLast && nextSibling != null)
        dropLast(1)
      else
        this
    }
}