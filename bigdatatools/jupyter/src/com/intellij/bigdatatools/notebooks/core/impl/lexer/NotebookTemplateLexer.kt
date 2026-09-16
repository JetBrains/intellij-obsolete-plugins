package com.intellij.bigdatatools.notebooks.core.impl.lexer

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.lexer.MergeFunction
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

abstract class NotebookTemplateLexer(cellTypeAwareLexer: NotebookCellTypeAwareLexer, private val tokenSet: TokenSet) : MergingLexerAdapter(cellTypeAwareLexer, tokenSet) {
  var notebook: BasicNotebook? = null
  override fun getMergeFunction(): MergeFunction = MergeFunction { type, originalLexer ->
    if (!tokenSet.contains(type)) {
      return@MergeFunction type
    }

    while (true) {
      val tokenType = originalLexer.tokenType
      if (!tokenSet.contains(tokenType)) break
      originalLexer.advance()
    }

    if (originalLexer is NotebookCellTypeAwareLexer) {
      return@MergeFunction selectElementTypeByCurrentCellType(originalLexer)
    }
    return@MergeFunction rawSource
  }

  protected abstract fun selectElementTypeByCurrentCellType(originalLexer: NotebookCellTypeAwareLexer): IElementType

  protected abstract val rawSource: IElementType
}