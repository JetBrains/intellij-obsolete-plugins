package com.intellij.bigdatatools.zeppelin.editor.actions

import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookClearCellOutputAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookClearOutputsAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookCloneCellAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookDeleteCellAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionBase
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookInsertCellAboveAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookInsertCellBelowAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookMergeCellAboveAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookMoveCellAboveAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookMoveCellBelowAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookRunAllAboveAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookRunAllBelowAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookRunCellAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookRunCellSelectBelowAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookSelectCellAboveAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookSelectCellBelowAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookSplitCellAction
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookStopCellAction
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware

/**
 * Marker interface to distinguish Zeppelin actions invoked in editor from all other actions.
 *
 * May be used e.g. by [ActionPromoter].
 */
interface ZeppelinEditorAction : DumbAware

class ZeppelinRestartInterpreterAction : NotebookEditorActionBase(), ZeppelinEditorAction {
  override fun actionPerformed(event: AnActionEvent) = service.restartInterpreter(event)

  companion object {
    const val ID = "ZeppelinRestartCellAction"
  }
}

class ZeppelinRunCellAction : NotebookRunCellAction(), ZeppelinEditorAction {
  private var lastRunPressed: Long = 0

  override fun actionPerformed(event: AnActionEvent) {
    if (System.currentTimeMillis() - lastRunPressed < 1000)
      return

    lastRunPressed = System.currentTimeMillis()
    FileDocumentManager.getInstance().saveAllDocuments()
    service.runCell(event)
  }

  companion object {
    const val ID = "ZeppelinRunCellAction"
  }
}

class ZeppelinRunAllBelowAction : NotebookRunAllBelowAction(), ZeppelinEditorAction {
  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabled = event.presentation.isEnabled && !service.hasRunningCells(event)
  }

  companion object {
    const val ID = "ZeppelinRunAllBelowAction"
  }
}

class ZeppelinRunAllAboveAction : NotebookRunAllAboveAction(), ZeppelinEditorAction {
  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabled = event.presentation.isEnabled && !service.hasRunningCells(event)
  }

  companion object {
    const val ID = "ZeppelinRunAllAboveAction"
  }
}

class ZeppelinClearAllOutputAction : NotebookClearOutputsAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinClearOutputs"
  }
}

class ZeppelinClearCellOutputAction : NotebookClearCellOutputAction(), ZeppelinEditorAction

class ZeppelinCloneCellAction : NotebookCloneCellAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinCloneCellAction"
  }
}

class ZeppelinToggleCellTitleAction : NotebookEditorActionBase(), ZeppelinEditorAction {

  override fun update(event: AnActionEvent) {
    super.update(event)
    val cell = event.noteCell ?: return
    event.presentation.text = if (cell.titleVisible) ZepMessagesBundle.message("action.cell.title.hide")
    else ZepMessagesBundle.message("action.ZeppelinToggleCellTitle.text")
  }

  override fun actionPerformed(e: AnActionEvent) = service.renameCellTitle(e)

  companion object {
    const val ID = "ZeppelinToggleCellTitle"
  }
}

class ZeppelinStopCellAction : NotebookStopCellAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinStopCellAction"
  }
}

class ZeppelinRunCellSelectBelowAction : NotebookRunCellSelectBelowAction(), ZeppelinEditorAction

class ZeppelinSelectCellAboveAction : NotebookSelectCellAboveAction(), ZeppelinEditorAction

class ZeppelinSelectCellBelowAction : NotebookSelectCellBelowAction(), ZeppelinEditorAction

class ZeppelinInsertCellAboveAction : NotebookInsertCellAboveAction(), ZeppelinEditorAction

class ZeppelinInsertCellBelowAction : NotebookInsertCellBelowAction(), ZeppelinEditorAction

class ZeppelinDeleteCellAction : NotebookDeleteCellAction(), ZeppelinEditorAction

class ZeppelinMoveCellAboveAction : NotebookMoveCellAboveAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinMoveCellAboveAction"
  }
}

class ZeppelinMergeCellAction : NotebookMergeCellAboveAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinMergeCellsAction"
  }
}

class ZeppelinSplitCellAction : NotebookSplitCellAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinSplitCellAction"
  }
}

class ZeppelinMoveCellBelowAction : NotebookMoveCellBelowAction(), ZeppelinEditorAction {
  companion object {
    const val ID = "ZeppelinMoveCellBelowAction"
  }
}