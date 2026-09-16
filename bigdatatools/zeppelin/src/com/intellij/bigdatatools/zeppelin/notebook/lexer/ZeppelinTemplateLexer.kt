// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.notebook.lexer

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.core.impl.lexer.NotebookCellTypeAwareLexer
import com.intellij.bigdatatools.notebooks.core.impl.lexer.NotebookTemplateLexer
import com.intellij.bigdatatools.notebooks.zeppelin.lang._ZeppelinLexer
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinCellTypes
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinTemplateTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class ZeppelinCellTypeAwareLexer(val project: Project?)
  : NotebookCellTypeAwareLexer(_ZeppelinLexer()) {
  override val cellTypeValues: List<NotebookCellType> = ZeppelinCellTypes.values()
}

class ZeppelinTemplateLexer(project: Project?) : NotebookTemplateLexer(
  ZeppelinCellTypeAwareLexer(project), tokenSet) {
  override val rawSource: IElementType = ZeppelinTypes.RAW_SOURCE

  override fun selectElementTypeByCurrentCellType(originalLexer: NotebookCellTypeAwareLexer): IElementType = rawSource

  companion object {
    val tokenSet = TokenSet.create(ZeppelinTypes.CELL_MAGIC, ZeppelinTypes.ANY, ZeppelinTemplateTypes.NEWLINE)
  }
}