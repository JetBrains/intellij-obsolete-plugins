package com.intellij.bigdatatools.zeppelin.editor.gutter

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinCloneCellAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinMoveCellAboveAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinMoveCellBelowAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRestartInterpreterAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunAllAboveAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunAllBelowAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunCellAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinStopCellAction
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinToggleCellTitleAction
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinStopAllAction
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

internal class ZeppelinRunGutterIconRender(val cell: ZeppelinCell, val zeppelinEditor: ZeppelinEditor) : GutterIconRenderer() {
  private val runCellAction = ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinRunCellAction.ID)
  private val stopCellAction = ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinStopCellAction.ID)

  override fun getClickAction(): AnAction = if (cell.isLaunched)
    stopCellAction
  else
    runCellAction

  override fun getPopupMenuActions(): ActionGroup {
    val actionGroup = if (!cell.isLaunched) {
      DefaultActionGroup(runCellAction,
                         ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinRunAllAboveAction.ID),
                         ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinRunAllBelowAction.ID))
    }
    else {
      DefaultActionGroup(stopCellAction,
                         ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinStopAllAction(zeppelinEditor)))
    }

    actionGroup.addAll(Separator(),
                       ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinMoveCellAboveAction.ID),
                       ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinMoveCellBelowAction.ID),
                       Separator(),
                       ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinCloneCellAction.ID),
                       ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinToggleCellTitleAction.ID),
                       ZeppelinGutterAction(zeppelinEditor, cell, ZeppelinRestartInterpreterAction.ID))

    return actionGroup
  }

  override fun getIcon(): Icon = when (cell.status) {
    CellStatus.PENDING, CellStatus.RUNNING -> AllIcons.Actions.Suspend
    CellStatus.ERROR -> AllIcons.RunConfigurations.TestState.Red2
    CellStatus.ABORT -> AllIcons.RunConfigurations.TestState.Yellow2
    CellStatus.FINISHED -> AllIcons.RunConfigurations.TestState.Green2
    else -> AllIcons.RunConfigurations.TestState.Run
  }

  override fun getTooltipText() = when (cell.status) {
    CellStatus.PENDING, CellStatus.RUNNING -> ZepMessagesBundle.message("paragraph.state.cancel.tooltip")
    CellStatus.ERROR -> ZepMessagesBundle.message("paragraph.state.error.tooltip")
    CellStatus.ABORT -> ZepMessagesBundle.message("paragraph.state.abort.tooltip")
    CellStatus.FINISHED -> ZepMessagesBundle.message("paragraph.state.success.tooltip")
    else -> ZepMessagesBundle.message("paragraph.state.run.tooltip")
  }

  override fun isDumbAware(): Boolean = true

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ZeppelinRunGutterIconRender

    return cell == other.cell
  }

  override fun hashCode(): Int = cell.hashCode()
}