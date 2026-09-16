package com.intellij.bigdatatools.zeppelin.editor.actions

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteEditor
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.notebookVirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiDocumentManager

class ZeppelinSelectAllAction : AnAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.noteEditor != null && e.project != null && e.notebookVirtualFile != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.noteEditor ?: return
    val notebookVirtualFile = editor.notebookVirtualFile ?: return
    val project = e.project ?: return

    val caretOffset = editor.caretModel.offset

    PsiDocumentManager.getInstance(project).commitDocument(editor.document)

    val cells = notebookVirtualFile.notebook.cells

    val cellUnderCaret = cells.find { it.textRange.contains(caretOffset) } ?: cells.last()
    val cellTextRange = NotebookEditorUtils.getCellRangeInEditor(editor, cellUnderCaret)

    if (cellTextRange.startOffset == editor.selectionModel.selectionStart && cellTextRange.endOffset == editor.selectionModel.selectionEnd) {
      editor.selectionModel.setSelection(NotebookConstants.PARAGRAPH_DELIMITER.length, editor.document.textLength)
    }
    else {
      editor.selectionModel.setSelection(cellTextRange.startOffset, cellTextRange.endOffset)
    }
  }
}