package com.intellij.bigdatatools.zeppelin.integration.intentions

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataKeys
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.getPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull

abstract class ZeppelinIntentionBase(@NotNull @IntentionName text: String) :
  BaseIntentionAction(), PriorityAction, DumbAware {

  init {
    setText(text)
  }

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = psiFile.viewProvider.fileType is ZeppelinFileType

  companion object {
    private fun createActionEvent(editor: Editor, cell: NotebookCell): AnActionEvent {
      val dataContext = SimpleDataContext.builder()
        .add(NotebookDataKeys.NOTE_EDITOR, editor as? EditorImpl)
        .add(NotebookDataKeys.NOTE, cell.note)
        .add(NotebookDataKeys.NOTE_CELL, cell)
        .add(CommonDataKeys.EDITOR, editor)
        .add(CommonDataKeys.PROJECT, editor.project)
        .add(CommonDataKeys.PSI_FILE, editor.getPsiFile())
        .build()

      return AnActionEvent.createFromDataContext("", null, dataContext)
    }

    private fun performCellAction(editor: Editor, cell: NotebookCell, actionName: String) {
      val executeCellAction = ActionManager.getInstance().getAction(actionName)
      val actionEvent = createActionEvent(editor, cell)
      executeCellAction.actionPerformed(actionEvent)
    }

    fun performCurCellAction(editor: Editor, actionName: String) {
      val currentCell = getCurrentCell(editor) ?: return
      performCellAction(editor, currentCell, actionName)
    }

    private fun getNotebook(editor: Editor): BasicNotebook? {
      val notebookVirtualFile = (FileDocumentManager.getInstance().getFile(editor.document) ?: return null) as? NotebookVirtualFile
                                ?: return null
      return notebookVirtualFile.notebook
    }

    private fun getCurrentCell(editor: Editor, notebook: BasicNotebook): NotebookCell? {
      if (editor.isDisposed)
        return null
      val offset = editor.logicalPositionToOffset(editor.caretModel.logicalPosition)
      return notebook.getCellByOffset(offset) ?: notebook.cells.lastOrNull()
    }

    fun isCurrentCellNotFirst(editor: Editor): Boolean {
      val notebook = getNotebook(editor) ?: return false
      val cell = getCurrentCell(editor, notebook) ?: return false
      return cell != notebook.cells.first()
    }

    fun isCurrentCellNotLast(editor: Editor): Boolean {
      val notebook = getNotebook(editor) ?: return false
      val cell = getCurrentCell(editor, notebook) ?: return false
      return cell != notebook.cells.lastOrNull()
    }

    fun getCurrentCell(editor: Editor): NotebookCell? {
      if (editor.isDisposed) return null
      val notebook = getNotebook(editor) ?: return null
      return getCurrentCell(editor, notebook)
    }
  }
}