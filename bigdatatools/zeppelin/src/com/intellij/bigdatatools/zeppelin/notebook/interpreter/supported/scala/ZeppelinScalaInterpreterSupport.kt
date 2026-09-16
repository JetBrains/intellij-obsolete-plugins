package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.formatter.ZeppelinFormatterUtil
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupportEx
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.ZeppelinTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala.impl.ZeppelinScalaStructureViewDelegate
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingMode
import com.intellij.formatting.Indent
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.highlighter.ScalaSyntaxHighlighterFactory
import org.jetbrains.plugins.scala.lang.formatting.ScalaBlock
import org.jetbrains.plugins.scala.lang.formatting.SubBlocksContext
import org.jetbrains.plugins.scala.structureView.element.`Element$`
import scala.`None$`
import scala.Option

class ZeppelinScalaInterpreterSupport : InterpreterSupportEx() {
  override val id: String
    get() = SparkElementTypes.id

  override val language: Language
    get() = ScalaLanguage.INSTANCE

  override val template: NotebookTemplateDataElementType
    get() = ZeppelinScalaTemplate

  override fun syntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
    ScalaSyntaxHighlighterFactory().getSyntaxHighlighter(project, virtualFile)

  @Suppress("UNCHECKED_CAST")
  override fun getFormattingBlocks(notebookPsiFile: NotebookPsiFile, settings: CodeStyleSettings, mode: FormattingMode): List<Block> {
    val scalaFile = notebookPsiFile.viewProvider.getPsi(ScalaLanguage.INSTANCE)!!
    val spitedElementsByCells = ZeppelinFormatterUtil.splitByOuter(scalaFile.children.toList())
    val cellsBlocks = getScalaBlocks(spitedElementsByCells, settings)
    val cellSources = getSourceCellsFor(notebookPsiFile, interpreterTypes.marker)
    return formatByCells(cellsBlocks, cellSources)
  }

  override fun isFileStructureEnabled(): Boolean = true

  override fun createDelegate(startElement: PsiElement,
                              result: MutableList<TreeElement>) {
    if (startElement.language != ScalaLanguage.INSTANCE)
      return
    val te = `Element$`.`MODULE$`.forPsi(startElement, false)
    if (te.nonEmpty()) {
      te.head()?.let { // todo other languages too
        result.add(ZeppelinScalaStructureViewDelegate(it))
      }
    }
  }

  private fun getScalaBlocks(spitedElementsByCells: List<List<PsiElement>>,
                             settings: CodeStyleSettings): List<ScalaBlock> {
    return spitedElementsByCells.mapNotNull {
      //val start = it.firstOrNull() { it !is PsiWhiteSpace } ?: return@mapNotNull null
      val start = it.firstOrNull() ?: return@mapNotNull null
      val end = it.lastOrNull() ?: return@mapNotNull null
      //val end = it.lastOrNull { it !is PsiWhiteSpace } ?: return@mapNotNull null
      ScalaBlock(start.node, end.node, null, Indent.getAbsoluteNoneIndent(),
                 null, settings, `None$`.`MODULE$` as Option<SubBlocksContext>)
    }
  }
}

private object ZeppelinScalaTemplate : ZeppelinTemplateDataElementType(
  "ZEPPELIN_SCALA_TEMPLATE",
  SparkElementTypes.source
) {
  override fun getTemplateFileLanguage(viewProvider: TemplateLanguageFileViewProvider?): Language = ScalaLanguage.INSTANCE
}