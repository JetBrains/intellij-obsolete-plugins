package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.bash

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupportEx
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.ZeppelinTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw.RawTextBlock
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingMode
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.sh.ShLanguage
import com.intellij.sh.highlighter.ShSyntaxHighlighter

internal class ZeppelinBashInterpreter : InterpreterSupportEx() {
  override val id: String
    get() = BashTypes.id

  override val template: NotebookTemplateDataElementType
    get() = ZeppelinBashTemplate

  override val language: Language
    get() = ShLanguage.INSTANCE

  override fun syntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = ShSyntaxHighlighter()

  override fun getFormattingBlocks(notebookPsiFile: NotebookPsiFile, settings: CodeStyleSettings, mode: FormattingMode): List<Block> {
    val cellSources = getSourceCellsFor(notebookPsiFile, interpreterTypes.marker)
    return cellSources.map { RawTextBlock(it.node) }
  }
}

private object ZeppelinBashTemplate : ZeppelinTemplateDataElementType(
  "ZEPPELIN_BASH_TEMPLATE",
  BashTypes.source
) {
  override fun getTemplateFileLanguage(
    viewProvider: TemplateLanguageFileViewProvider?): Language = ShLanguage.INSTANCE
}