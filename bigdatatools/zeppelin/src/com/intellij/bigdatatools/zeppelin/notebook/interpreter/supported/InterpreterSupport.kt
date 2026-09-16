package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiSource
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.formatter.ZeppelinMarkerBlock
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinMarkerResolver
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinSupportLanguages
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw.RawTextBlock
import com.intellij.bigdatatools.zeppelin.notebook.lexer.ZeppelinFileBasedTemplateLexer
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinTemplateTypes
import com.intellij.bigdatatools.zeppelin.refactoring.ZeppelinExtractRefactoringUtil
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingMode
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.originalFile
import com.intellij.psi.DummyBlockType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

interface InterpreterSupport {
  val id: String
  val language: Language
  val template: NotebookTemplateDataElementType

  fun syntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter?
  fun ignoreInspection(file: PsiFile) {}

  fun getFormattingBlocks(notebookPsiFile: NotebookPsiFile,
                          settings: CodeStyleSettings,
                          mode: FormattingMode): List<Block> = emptyList()

  fun getStructureElements(psiCell: PsiCell): List<TreeElement> = emptyList()

  companion object {
    private const val ID: String = "com.intellij.bigdatatools.zeppelin.interpreterSupport"
    private val EP_NAME = ExtensionPointName.create<InterpreterSupport>(ID)
    fun getInterpreters() = EP_NAME.extensionList.toList()

    fun getStructureElements(psiCell: PsiCell): List<TreeElement> {
      val marker = psiCell.cellMarker.firstChild
      val id = ZeppelinSupportLanguages.markerToIds[marker.elementType] ?: return emptyList()
      val interpreter = getInterpreters().firstOrNull { it.id == id } ?: return emptyList()
      return interpreter.getStructureElements(psiCell)
    }
  }
}

fun PsiCell.markerLanguage(): Language? {
  val marker = this.cellMarker.firstChild
  val id = ZeppelinSupportLanguages.markerToIds[marker.elementType] ?: return null
  val interpreter = InterpreterSupport.getInterpreters().firstOrNull { it.id == id } ?: return null
  return interpreter.language

}

abstract class InterpreterSupportEx : InterpreterSupport {
  protected val interpreterTypes: InterpreterTypes by lazy {
    ZeppelinSupportLanguages.idToTypes[id] ?: error("Interpreter types have not found")
  }

  override fun getFormattingBlocks(notebookPsiFile: NotebookPsiFile, settings: CodeStyleSettings, mode: FormattingMode): List<Block> {
    val langElementsInSources = getLangElementsInNote(notebookPsiFile)
    return langElementsInSources.map {
      ZeppelinMarkerBlock(it.node)
    }
  }

  open fun isFileStructureEnabled(): Boolean = false

  override fun getStructureElements(psiCell: PsiCell): List<TreeElement> {
    if (!isFileStructureEnabled() || psiCell.firstChild == null) return emptyList()

    val psiFile = psiCell.containingFile.viewProvider.getPsi(language)!!
    val desiredPsiElement = psiFile.findElementAt(psiCell.textRange.startOffset + psiCell.firstChild.textLength) ?: return emptyList()
    var startElement: PsiElement? = ZeppelinExtractRefactoringUtil.getTopLevelElement(desiredPsiElement)

    val result = mutableListOf<TreeElement>()

    while (startElement != null && startElement.textRange.endOffset <= psiCell.textRange.endOffset) {
      createDelegate(startElement, result)

      startElement = startElement.nextSibling
    }

    return result.toList()
  }

  protected open fun createDelegate(startElement: PsiElement, result: MutableList<TreeElement>) {}

  private fun getLangElementsInNote(notebookPsiFile: NotebookPsiFile): List<PsiElement> {
    val cellSources = getSourceCellsFor(notebookPsiFile, interpreterTypes.marker)
    val cellSourcesRanges = cellSources.map { it.textRange }.toSet()

    val langPsiElements = getLangPsiElements(notebookPsiFile).filter { it.text.isNotBlank() }
    val flattenDummy = langPsiElements.flatMap {
      if (it is DummyBlockType.DummyBlock)
        it.getChildren().toList()
      else
        listOf(it)
    }
    return flattenDummy.filter {
      val start = it.textRange.startOffset
      val end = it.textRange.endOffset - 1
      cellSourcesRanges.any { sourceRange -> sourceRange.contains(start) && sourceRange.contains(end) }
    }
  }

  protected fun getFixedLangBlocks(notebookPsiFile: NotebookPsiFile, constructor: BlockConstructor): List<Block> {
    val cellSources = getSourceCellsFor(notebookPsiFile, interpreterTypes.marker)
    val blocks = getLangElementsInNote(notebookPsiFile).map { constructor.createBlock(it) }

    return formatByCells(blocks, cellSources)
  }

  protected open fun getLangPsiElements(notebookPsiFile: NotebookPsiFile): List<PsiElement> {
    val langFile = notebookPsiFile.viewProvider.getPsi(language)!!
    return langFile.children.filter { it !is OuterLanguageElement && it !is PsiWhiteSpace }
  }

  protected fun getSourceCellsFor(notebookPsiFile: NotebookPsiFile, vararg desireMarkers: IElementType): List<PsiSource> {
    val file = notebookPsiFile.virtualFile ?: notebookPsiFile.viewProvider.virtualFile.originalFile()
    if (file == null) return emptyList()
    val configId = NotebookFileUtil.getConfigId(file)
    val notebookId = NotebookFileUtil.getNotebookId(file)

    return notebookPsiFile.cells.mapNotNull {
      val marker = ZeppelinMarkerResolver.getMarkerByText(it.cellMarker.text, configId, notebookId)
      if (marker in desireMarkers)
        it.source
      else
        null
    }
  }

  protected fun formatByCells(cellsBlocks: List<Block>, cellSources: List<PsiSource>): List<Block> {
    val cellsToBlocks = computeCellsToBlocks(cellsBlocks, cellSources)

    return cellSources.flatMap { psiSource ->
      val blocksForCell = cellsToBlocks.getOrDefault(psiSource.textRange, mutableListOf())
      if (blocksForCell.isEmpty() && psiSource.text.isNotBlank() ||
          isContainNotCoveredTextInCell(psiSource, blocksForCell))
        listOf(RawTextBlock(psiSource.node))
      else
        blocksForCell
    }
  }

  private fun isContainNotCoveredTextInCell(psiSource: PsiSource,
                                            blocksForCell: MutableList<Block>): Boolean {
    val cellRange = psiSource.textRange
    val borderRanges = listOf(
      TextRange(cellRange.startOffset - 1, cellRange.startOffset),
      TextRange(cellRange.endOffset, cellRange.endOffset + 1)
    )
    val allRanges = (borderRanges + blocksForCell.map { it.textRange }).sortedBy { it.startOffset }
    val emptySpaces = getNotCoveredRanges(allRanges)
    return emptySpaces.any { notCoveredRange ->
      psiSource.text
        .substring(notCoveredRange.startOffset - psiSource.textOffset,
                   notCoveredRange.endOffset - psiSource.textOffset)
        .isNotBlank()
    }
  }

  private fun getNotCoveredRanges(allRanges: List<TextRange>): List<TextRange> {
    return (1 until allRanges.size).mapNotNull { index ->
      val prev = allRanges[index - 1]
      val cur = allRanges[index]
      if (prev.endOffset != cur.startOffset) {
        TextRange(prev.endOffset, cur.startOffset)
      }
      else {
        null
      }
    }
  }


  private fun computeCellsToBlocks(blocks: List<Block>, psiCells: List<PsiSource>): MutableMap<TextRange, MutableList<Block>> {
    val cellsToBlocks = mutableMapOf<TextRange, MutableList<Block>>()
    var blockIndex = 0
    var cellIndex = 0
    while (blockIndex < blocks.size || cellIndex < psiCells.size) {
      val block = blocks.getOrNull(blockIndex)
      val cell = psiCells.getOrNull(cellIndex)
      if (block == null) {
        cellIndex++
        continue
      }
      if (cell == null) {
        blockIndex++
        continue
      }

      val cellRange = cell.textRange
      val blockRange = block.textRange
      if (cellRange.contains(blockRange)) {
        val ranges = cellsToBlocks.getOrPut(cellRange) { mutableListOf() }
        ranges.add(block)
        blockIndex++
        continue
      }
      //make cell formatting blocks empty if we have element which intersects but not in cell range
      if (cellRange.intersects(blockRange)) {
        cellsToBlocks[cellRange] = mutableListOf()
        blockIndex++
        cellIndex++
        continue
      }

      cellsToBlocks.getOrPut(cellRange) { mutableListOf() }
      cellIndex++
    }
    return cellsToBlocks
  }

  protected interface BlockConstructor {
    fun createBlock(psiElement: PsiElement): Block
  }
}

abstract class ZeppelinTemplateDataElementType(debugName: String, templateElementType: IElementType) :
  NotebookTemplateDataElementType(debugName, templateElementType, ZeppelinLanguage, ZeppelinTemplateTypes.OUTER) {
  override fun getTemplateLexer(project: Project, virtualFile: VirtualFile?) =
    ZeppelinFileBasedTemplateLexer(project, virtualFile)
}