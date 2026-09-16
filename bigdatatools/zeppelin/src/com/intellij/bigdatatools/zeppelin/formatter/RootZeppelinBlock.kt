package com.intellij.bigdatatools.zeppelin.formatter

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupport
import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.FormattingMode
import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings

class RootZeppelinBlock(private val notebookPsiFile: NotebookPsiFile, val settings: CodeStyleSettings, val mode: FormattingMode)
  : StubBlock(notebookPsiFile.textRange), ASTBlock {
  private var cachedSubBlocks: List<Block>? = null

  val project = notebookPsiFile.project
  val cells = notebookPsiFile.cells

  override fun getNode(): ASTNode = notebookPsiFile.node

  override fun isLeaf(): Boolean = false
  override fun getTextRange(): TextRange = notebookPsiFile.textRange

  override fun getSubBlocks(): MutableList<Block> = cachedSubBlocks?.toMutableList() ?: createSubBlocks()

  private fun createSubBlocks(): MutableList<Block> {
    val interpretersFormattingBlocks = InterpreterSupport.getInterpreters().flatMap {
      it.getFormattingBlocks(notebookPsiFile, settings, mode)
    }

    val markerBlocks = getMarkerBlocks()
    val allCalculatedBlocks = (interpretersFormattingBlocks + markerBlocks).sortedBy { it.textRange.startOffset }

    cachedSubBlocks = allCalculatedBlocks
    return allCalculatedBlocks.toMutableList()
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes =
    ChildAttributes(Indent.getAbsoluteNoneIndent(), null)

  private fun getMarkerBlocks() = notebookPsiFile.cells.map {
    ZeppelinMarkerBlock(it.cellMarker.node)
  }
}