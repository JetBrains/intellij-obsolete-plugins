/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinEditorAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.openapi.editor.actions.CtrlYActionChooser
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.editor.actions.TextComponentEditorAction
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.TextRange

/**
 * Migrated from DeleteLineAction
 */
object NoteDeleteLineAction : TextComponentEditorAction(Handler()), ZeppelinEditorAction {
  private fun getRangeToDelete(editor: Editor, caret: Caret): TextRange {
    val selectionStart = caret.selectionStart
    var selectionEnd = caret.selectionEnd
    var startOffset = EditorUtil.getNotFoldedLineStartOffset(editor, selectionStart, true)
    // There is a possible case that selection ends at the line start, i.e. something like below ([...] denotes selected text,
    // '|' is a line start):
    //   |line 1
    //   |[line 2
    //   |]line 3
    // We don't want to delete line 3 here. However, the situation below is different:
    //   |line 1
    //   |[line 2
    //   |line] 3
    // Line 3 must be removed here.
    if (selectionEnd > 0 && selectionEnd != selectionStart) selectionEnd--
    var endOffset = EditorUtil.getNotFoldedLineEndOffset(editor, selectionEnd, true)
    if (endOffset < editor.document.textLength) {
      endOffset++
    }
    else if (startOffset > 0) {
      startOffset--
    }
    return TextRange(startOffset, endOffset)
  }

  /**
   * try to replace delete operation with equivalent if original operation forbidden due to guarded block
   * In text "\na\n" with guarded last symbol "\n" deleteString(1, 3) will be replaced with deleteString(0, 2).
   * equivalent replace are searched to the left from original
   */
  fun deleteString(editor: Editor, document: Document, startOffset: Int, endOffset: Int) {
    val notebookVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) as? NotebookVirtualFile ?: return
    val note = notebookVirtualFile.notebook
    val startCell = note.getCellByOffset(startOffset) ?: error("Internal error. Cannot find cell for removing line")
    val endCell = note.getCellByOffset(endOffset)
    if (startCell.textOffset > startOffset) {
      return
    }

    val (realStartOffset, realEndOffset) = when {
      startCell == endCell -> startOffset to endOffset
      endOffset == document.textLength -> (startOffset).coerceAtLeast(startCell.textOffset) to endOffset
      else -> (startOffset - 1).coerceAtLeast(startCell.textOffset) to endOffset - 1

    }

    if (realStartOffset > realEndOffset)
      return
    try {
      document.deleteString(realStartOffset, realEndOffset)
    }
    catch (ex: ReadOnlyFragmentModificationException) {
      val block = ex.guardedBlock
      if (block.endOffset < realEndOffset) {
        throw ex
      }
      val blockPrefix = document.immutableCharSequence.subSequence(block.startOffset, realEndOffset)
      val textBefore = document.immutableCharSequence.subSequence(0.coerceAtLeast(realStartOffset - blockPrefix.length), realStartOffset)
      if (blockPrefix.isNotEmpty() && blockPrefix.toString() == textBefore.toString()) {
        deleteString(editor, document, realStartOffset - blockPrefix.length, realEndOffset - blockPrefix.length)
      }
      else {
        throw ex
      }
    }
  }

  private class Handler : EditorWriteActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
      if (CtrlYActionChooser.isCurrentShortcutOk(dataContext)) super.doExecute(editor, caret, dataContext)
    }

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
      CommandProcessor.getInstance().currentCommandGroupId = EditorActionUtil.DELETE_COMMAND_GROUP
      CopyPasteManager.getInstance().stopKillRings()
      val carets = caret?.let { listOf(it) } ?: editor.caretModel.allCarets
      editor.caretModel.runBatchCaretOperation {
        val caretColumns = IntArray(carets.size)
        var caretIndex = carets.size - 1
        var range = getRangeToDelete(editor, carets[caretIndex])
        while (caretIndex >= 0) {
          val currentCaretIndex = caretIndex
          var currentRange = range
          // find carets with overlapping line ranges
          while (--caretIndex >= 0) {
            range = getRangeToDelete(editor, carets[caretIndex])
            if (range.endOffset < currentRange.startOffset) {
              break
            }
            currentRange = TextRange(range.startOffset, currentRange.endOffset)
          }
          for (i in caretIndex + 1..currentCaretIndex) {
            caretColumns[i] = carets[i].visualPosition.column
          }
          val targetLine = editor.offsetToVisualPosition(currentRange.startOffset).line
          deleteString(editor, editor.document, currentRange.startOffset, currentRange.endOffset)
          for (i in caretIndex + 1..currentCaretIndex) {
            carets[i].moveToVisualPosition(VisualPosition(targetLine, caretColumns[i]))
          }
        }
      }
      editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
    }
  }
}