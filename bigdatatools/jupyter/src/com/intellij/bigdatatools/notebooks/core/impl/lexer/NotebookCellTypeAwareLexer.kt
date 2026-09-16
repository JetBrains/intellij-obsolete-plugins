package com.intellij.bigdatatools.notebooks.core.impl.lexer

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.FlexLexer

abstract class NotebookCellTypeAwareLexer(lexer: FlexLexer) : FlexAdapter(lexer) {
  abstract val cellTypeValues: List<NotebookCellType>
  var currentCellType: NotebookCellType = NotebookCellType.UNDEFINED

  final override fun getState(): Int = cellTypeValues.indexOf(currentCellType)
  final override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    currentCellType = cellTypeValues[initialState]
    super.start(buffer, startOffset, endOffset, 0)
  }
}