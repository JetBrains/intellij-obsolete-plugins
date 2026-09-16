package com.intellij.bigdatatools.zeppelin.notebook.lexer

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.core.impl.lexer.NotebookCellTypeAwareLexer
import com.intellij.bigdatatools.notebooks.core.impl.lexer.NotebookTemplateLexer
import com.intellij.bigdatatools.notebooks.zeppelin.lang._ZeppelinLexer
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinCellTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinMarkerResolver
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinSupportLanguages
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinTemplateTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class ZeppelinFileBasedTemplateLexer(project: Project?, virtualFile: VirtualFile?) : NotebookTemplateLexer(
  ZeppelinFileBasedCellTypeAwareLexer(project, virtualFile), tokenSet) {
  override val rawSource: IElementType = ZeppelinTypes.RAW_SOURCE

  override fun selectElementTypeByCurrentCellType(originalLexer: NotebookCellTypeAwareLexer): IElementType =
    ZeppelinSupportLanguages.cellTypesToSources[originalLexer.currentCellType] ?: ZeppelinTypes.RAW_SOURCE

  companion object {
    val tokenSet = TokenSet.create(
      ZeppelinTypes.CELL_MAGIC,
      ZeppelinTypes.ANY,
      ZeppelinTemplateTypes.NEWLINE)
  }
}

class ZeppelinFileBasedCellTypeAwareLexer(val project: Project?, val virtualFile: VirtualFile?)
  : NotebookCellTypeAwareLexer(_ZeppelinLexer()) {
  override val cellTypeValues: List<NotebookCellType> = ZeppelinCellTypes.values()

  override fun advance() {
    var token = tokenType
    val tokenText = tokenText

    if (token == ZeppelinTypes.CODE_MARKER) {
      val notebookId = virtualFile?.let { NotebookFileUtil.getNotebookId(it) }
      val configId = virtualFile?.let { NotebookFileUtil.getConfigId(it) }
      token = ZeppelinMarkerResolver.getMarkerByText(tokenText, configId, notebookId)
    }

    ZeppelinSupportLanguages.markerToCellTypes[token]?.let {
      currentCellType = it
    }
    super.advance()
  }
}