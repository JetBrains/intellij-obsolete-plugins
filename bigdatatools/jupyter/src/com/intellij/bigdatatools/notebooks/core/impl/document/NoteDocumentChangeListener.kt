package com.intellij.bigdatatools.notebooks.core.impl.document

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange

class NoteDocumentChangeListener(private val noteFile: NotebookVirtualFile,
                                 private val project: Project,
                                 val note: BasicNotebook) : BulkAwareDocumentListener {
  private val customEventsApplier = NoteCustomEventsApplier()
  private var beforeAffectedCells = listOf<NotebookCell>()

  private val delimiter = note.delimiter
  private val fullDelimiter: String = "\n${note.delimiter}"
  private val fullDelimiterRegex = Regex(fullDelimiter)

  override fun bulkUpdateStarting(document: Document) = try {
    customEventsApplier.beforeChange(document)
  }
  catch (t: Throwable) {
    handleError(t)
  }

  override fun bulkUpdateFinished(document: Document) = try {
    customEventsApplier.afterChange(document)
  }
  catch (t: Throwable) {
    handleError(t)
  }

  override fun beforeDocumentChangeNonBulk(event: DocumentEvent) {
    try {
      if (NoteDocumentFileUtil.getIgnoreDocumentChange(event.document))
        return

      beforeAffectedCells = findBeforeAffectedCells(event)
    }
    catch (t: Throwable) {
      handleError(t)
    }
  }

  override fun documentChangedNonBulk(event: DocumentEvent) = documentChangedEx(event)

  fun documentChangedEx(event: DocumentEvent,
                        startNoteModification: Boolean = true,
                        endNoteModification: Boolean = true) = try {
    documentChangeExInner(event, startNoteModification, endNoteModification)
  }
  catch (t: Throwable) {
    handleError(t)
  }


  private fun documentChangeExInner(event: DocumentEvent,
                                    startNoteModification: Boolean,
                                    endNoteModification: Boolean) {
    if (NoteDocumentFileUtil.getIgnoreDocumentChange(event.document))
      return

    val oldCells = beforeAffectedCells
    beforeAffectedCells = listOf()

    val newCellTexts = findAfterAffectedCells(event)

    if (oldCells.isEmpty() && newCellTexts.isEmpty()) {
      logger.warn("Note changes are not found")
      return
    }

    val startChangeIndex = findIndexOfPrevAffectedCell(event) + 1
    val cellSizeAfterOps = note.cells.size - (oldCells.size - newCellTexts.size)
    if (cellSizeAfterOps < 1) {
      error("Cells after changes less that 1. Before: ${note.cells.size}, removed: ${oldCells.size}, added: ${newCellTexts.size}")
    }
    performNoteModification(startChangeIndex, newCellTexts, oldCells, startNoteModification, endNoteModification)
  }

  fun startNoteModification() {
    note.startModificationSection()
  }

  fun endNoteModification() {
    note.endModificationSection()
  }

  private fun performNoteModification(startChangeIndex: Int,
                                      newCellTexts: List<String>,
                                      oldCells: List<NotebookCell>,
                                      startNoteModification: Boolean,
                                      endNoteModification: Boolean) {
    if (startNoteModification)
      startNoteModification()

    innerPerformModification(oldCells, newCellTexts, startChangeIndex)

    if (endNoteModification)
      endNoteModification()
  }

  private fun innerPerformModification(oldCells: List<NotebookCell>,
                                       newCellTexts: List<String>,
                                       startChangeIndex: Int) {
    val needReplaceFirstCell = oldCells.isNotEmpty() && newCellTexts.isNotEmpty()
    if (needReplaceFirstCell) {
      removeCells(oldCells.drop(1))

      val changedCell = oldCells.first()
      val newChangeCellText = newCellTexts.first()
      changedCell.source = newChangeCellText

      addCells(newCellTexts.drop(1), startChangeIndex + 1)
    }
    else {
      removeCells(oldCells)
      addCells(newCellTexts, startChangeIndex)
    }
  }

  private fun findIndexOfPrevAffectedCell(event: DocumentEvent): Int {
    val leftCellOffset = findAffectedLeftCellOffsetAfter(event)
    return if (leftCellOffset == 0) -1 else note.cells.first { it.textRange.contains(leftCellOffset - 1) }.indexInNote
  }


  private fun findCellByStartOffset(start: Int): NotebookCell? {
    val cellRanges = note.cells.map { it.textRange }
    val cellIndex = cellRanges.withIndex().firstOrNull { it.value.startOffset == start }?.index
    return cellIndex?.let {
      note.cells[it]
    }
  }

  private fun findAffectedLeftCellOffsetBefore(event: DocumentEvent): Int {
    val changeEnd = event.offset
    return findLeftAffectedCellOffset(event, changeEnd, true)
  }

  private fun removeCells(cells: List<NotebookCell>) = cells.forEach {
    note.removeCell(it)
  }

  private fun addCells(cellTexts: List<String>, startIndex: Int) = cellTexts.withIndex().forEach {
    note.addNewCell(it.value, it.index + startIndex)
  }

  private fun getOldNewRightParts(text: String,
                                  offset: Int,
                                  event: DocumentEvent,
                                  before: Boolean): Pair<String, String> {
    return if (!before) {
      val newRight = text.substring(offset)
      val oldRight = event.oldFragment.toString() + newRight.removePrefix(event.newFragment)
      Pair(newRight, oldRight)
    }
    else {
      val oldRight = text.substring(offset)
      val newRight = event.newFragment.toString() + oldRight.removePrefix(event.oldFragment)
      Pair(newRight, oldRight)
    }

  }


  private fun findLeftAffectedCellOffset(event: DocumentEvent, changeStart: Int, before: Boolean): Int {
    val text = event.document.text
    val (newRight, oldRight) = getOldNewRightParts(text, changeStart, event, before)
    if (newRight.startsWith(delimiter) && oldRight.startsWith(delimiter) && (changeStart == 0 || text[changeStart - 1] == '\n'))
      return event.offset

    val leftNotChangedPart = text.substring(0, changeStart)
    val rightPath = if (before) oldRight else newRight
    val searchText = leftNotChangedPart + rightPath.substring(0, (delimiter.length - 1).coerceAtMost(rightPath.length))
    return searchText.lastIndexOf(fullDelimiter) + 1
  }


  private fun findAffectedLeftCellOffsetAfter(event: DocumentEvent): Int {
    val changeEnd = event.offset
    return findLeftAffectedCellOffset(event, changeEnd, false)
  }

  private fun findNextDelimiter(text: String, searchOffset: Int): Int? {
    if (text.length == searchOffset)
      return null
    if ((searchOffset == 0 || text[searchOffset - 1] == '\n') && text.substring(searchOffset).startsWith(delimiter))
      return searchOffset

    val indexOfDelimiter = text.substring(searchOffset).indexOf(fullDelimiter)
    if (indexOfDelimiter == -1)
      return null

    return indexOfDelimiter + 1 + searchOffset
  }


  private fun findAfterAffectedCells(event: DocumentEvent): List<String> {
    val eventDocument = event.document
    val leftCellOffset = findAffectedLeftCellOffsetAfter(event)
    var rightCellOffset = findNextDelimiter(event.document.text, event.offset + event.newLength)

    if (rightCellOffset != null && event.offset > 0 && event.newLength + event.offset == rightCellOffset) {
      val oldEndWithoutSlashN = event.oldLength > 0 && !event.oldFragment.endsWith('\n')
      val leftSymbolIsNotSlashN = event.oldLength == 0 && eventDocument.text[event.offset - 1] != '\n'
      if (oldEndWithoutSlashN || leftSymbolIsNotSlashN)
        rightCellOffset = findNextDelimiter(eventDocument.text, rightCellOffset + 1) ?: eventDocument.textLength
    }

    rightCellOffset = rightCellOffset ?: eventDocument.textLength

    val newAffectedCellsText = eventDocument.getText(TextRange(leftCellOffset, rightCellOffset))

    if (leftCellOffset == rightCellOffset)
      return emptyList()

    val isPasteInTheEnd = rightCellOffset == eventDocument.textLength
    val parts = newAffectedCellsText.split(fullDelimiterRegex)
    return parts.withIndex().map {
      var res = it.value
      if (it.index != 0)
        res = delimiter + res
      if (parts.lastIndex == it.index && !isPasteInTheEnd)
        res = res.dropLast(1)
      res
    }
  }


  private fun findBeforeAffectedCells(event: DocumentEvent): List<NotebookCell> {
    val text = event.document.text
    val offset = event.offset

    val leftCellOffset = findAffectedLeftCellOffsetBefore(event)
    val leftCellIndex = findCellByStartOffset(leftCellOffset)?.indexInNote ?: error(
      "Cannot get left cell By offset. Left offset: $leftCellOffset, offset: $offset, text:\n$text")

    val rightCellOffset = findNextDelimiter(text, offset + event.oldLength)
    var rightCell = rightCellOffset?.let { findCellByStartOffset(rightCellOffset) }

    val newSymbolBeforeRightCellDelimiter = when {
      rightCellOffset == null -> null
      rightCellOffset - 1 >= event.offset + event.oldLength -> event.document.charsSequence[rightCellOffset - 1]
      event.newLength > 0 -> event.newFragment.last()
      event.offset == 0 -> null
      else -> event.document.charsSequence[offset - 1]
    }

    val willNextDelimiterStartCell = newSymbolBeforeRightCellDelimiter == null || rightCellOffset == 0 || (rightCellOffset != null && newSymbolBeforeRightCellDelimiter == '\n')

    if (rightCell != null && !willNextDelimiterStartCell) {
      val index = rightCell.indexInNote
      if (index != note.cells.lastIndex)
        rightCell = note.cells[index + 1]
      else
        rightCell = null
    }


    return note.cells.drop(leftCellIndex).takeWhile { it != rightCell }
  }

  private fun handleError(t: Throwable) = NotebookEditorUtils.handleError(project, noteFile, t)

  companion object {
    fun registerNoteListener(noteFile: NotebookVirtualFile, project: Project, note: BasicNotebook, document: Document) {
      val listener = NoteDocumentChangeListener(noteFile, project, note)
      document.putUserData(KEY, listener)
      document.addDocumentListener(listener)
    }


    val KEY = Key<NoteDocumentChangeListener>("NOTE_DOCUMENT_CHANGE_LISTENER")
    private val logger = Logger.getInstance(this::class.java)
  }
}