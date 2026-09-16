package com.intellij.bigdatatools.zeppelin.notebook.parser

import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinMarkerResolver
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinSupportLanguages
import com.intellij.lang.PsiBuilder
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.FileContextUtil

object ZeppelinPsiRemapper {
  @JvmStatic
  fun remapCellMarker(psiBuilder: PsiBuilder) {
    val psiFile: PsiFile? = psiBuilder.getUserData(FileContextUtil.CONTAINING_FILE_KEY)
    val file = psiFile?.virtualFile
    val configId = file?.let { NotebookFileUtil.getConfigId(it) }
    val noteId = file?.let { NotebookFileUtil.getNotebookId(it) }
    val marker = ZeppelinMarkerResolver.getMarkerByText(psiBuilder.tokenText, configId, noteId)
    psiBuilder.remapCurrentToken(marker)
  }

  @JvmStatic
  fun remapCellSource(psiBuilder: PsiBuilder) {
    val prevMarker = psiBuilder.rawLookup(-1)
    val sourceForMarker = ZeppelinSupportLanguages.markerToSources[prevMarker] ?: return
    psiBuilder.remapCurrentToken(sourceForMarker)
  }
}