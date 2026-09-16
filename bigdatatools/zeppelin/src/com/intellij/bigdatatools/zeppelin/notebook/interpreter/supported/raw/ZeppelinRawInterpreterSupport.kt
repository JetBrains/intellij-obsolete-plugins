package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.formatter.ZeppelinMarkerBlock
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinSupportLanguages
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupport
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupportEx
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.ZeppelinTemplateDataElementType
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingMode
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider

class ZeppelinRawInterpreterSupport : InterpreterSupportEx() {
  override val id: String
    get() = RawElementTypes.id

  override val language: Language
    get() = PlainTextLanguage.INSTANCE

  override val template: NotebookTemplateDataElementType
    get() = ZeppelinRawTemplate

  override fun syntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = PlainSyntaxHighlighter()

  override fun getFormattingBlocks(notebookPsiFile: NotebookPsiFile, settings: CodeStyleSettings, mode: FormattingMode): List<Block> {
    val blocksForUnrecognizedCells = blocksForUnrecognizedCells(notebookPsiFile)
    val blocksForRawCells = super.getFormattingBlocks(notebookPsiFile, settings, mode)

    return blocksForRawCells + blocksForUnrecognizedCells
  }

  private fun blocksForUnrecognizedCells(notebookPsiFile: NotebookPsiFile): List<ZeppelinMarkerBlock> {
    val curEnabledSupportedLangs = InterpreterSupport.getInterpreters().map { it.id }
    val disabledLanguages = ZeppelinSupportLanguages.languageTypes.filter { it.id !in curEnabledSupportedLangs }
    val disabledMarkers = disabledLanguages.map { it.marker }.toTypedArray()
    val unrecognizedSources = getSourceCellsFor(notebookPsiFile, *disabledMarkers)

    return unrecognizedSources.map { ZeppelinMarkerBlock(it.node) }
  }
}

open class RawTextBlock(node: ASTNode) : AbstractBlock(node, null, null) {
  override fun isLeaf(): Boolean = true

  override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
  override fun buildChildren(): MutableList<Block> = mutableListOf()
}

private object ZeppelinRawTemplate : ZeppelinTemplateDataElementType("ZEPPELIN_RAW_TEMPLATE", RawElementTypes.source) {
  override fun getTemplateFileLanguage(viewProvider: TemplateLanguageFileViewProvider?): Language = PlainTextLanguage.INSTANCE
}