package com.intellij.bigdatatools.zeppelin.editor.gutter

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataKeys
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.getPsiFile
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction

internal class ZeppelinGutterAction(private val zeppelinEditor: ZeppelinEditor,
                                    private val cell: NotebookCell,
                                    private val action: AnAction) : DumbAwareAction() {
  constructor(zepEditor: ZeppelinEditor, cell: NotebookCell, actionId: String) :
    this(zepEditor, cell, ActionManager.getInstance().getAction(actionId))

  init {
    copyFrom(action)
  }

  /** Stores timestamp of last press of "Run cell" button. Timestamp is used to prevent double clicks and double runs. */
  private var lastRunPressed: Long = 0

  override fun actionPerformed(e: AnActionEvent) {
    // To prevent double tap on run button.
    if (System.currentTimeMillis() - lastRunPressed > 1000) {
      action.actionPerformed(createActionEvent(e, cell))
      lastRunPressed = System.currentTimeMillis()
    }
  }

  override fun update(e: AnActionEvent) {
    val actionEvent = createActionEvent(e, cell)
    action.update(actionEvent)
    e.presentation.isEnabled = actionEvent.presentation.isEnabled
    e.presentation.isVisible = actionEvent.presentation.isVisible
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun createActionEvent(e: AnActionEvent, cell: NotebookCell): AnActionEvent {
    val dataContext = SimpleDataContext.builder()
      .setParent(e.dataContext)
      .add(NotebookDataKeys.NOTE_EDITOR, zeppelinEditor.editor as? EditorImpl)
      .add(NotebookDataKeys.NOTE, cell.note)
      .add(NotebookDataKeys.NOTE_CELL, cell)
      .add(CommonDataKeys.EDITOR, zeppelinEditor.editor)
      .add(CommonDataKeys.PROJECT, zeppelinEditor.editor.project)
      .add(CommonDataKeys.PSI_FILE, zeppelinEditor.editor.getPsiFile())
      .build()

    return AnActionEvent.createFromInputEvent(e.inputEvent, "", null, dataContext)
  }
}