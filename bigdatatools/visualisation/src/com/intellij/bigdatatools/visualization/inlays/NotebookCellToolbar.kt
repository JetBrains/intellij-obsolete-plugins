package com.intellij.bigdatatools.visualization.inlays

import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.NotebookDataProvider
import com.intellij.bigdatatools.visualization.inlays.components.FadingToolbar
import com.intellij.bigdatatools.visualization.inlays.components.HoverPopupAction
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.inlays.style.InlaysToolbarStyle
import com.intellij.bigdatatools.visualization.inlays.utils.JobUrlHelper
import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ToolbarUtils
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.ui.components.ActionLink
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.BorderFactory
import javax.swing.border.Border

class NotebookCellToolbar(val inlay: NotebookInlayComponent,
                          private val actionsSupplier: ((NotebookInlayComponent) -> ActionGroup)) : ComponentListener {

  enum class CellToolbarPosition {
    TOP_RIGHT,
    BOTTOM_CENTER,
    TOP_CENTER
  }

  var toolbar: FadingToolbar? = null

  var position = CellToolbarPosition.TOP_RIGHT

  var border: Border = BorderFactory.createEmptyBorder(0, JBUIScale.scale(8), 0, JBUIScale.scale(8))

  fun show() {
    val toolbarTarget = ToolbarUtils.createTargetComponent(inlay) { sink ->
      NotebookDataProvider.uiDataSnapshot(sink, inlay.editor.project, inlay.editor, inlay.cell.note, inlay.cell)
    }
    val toolbar = FadingToolbar(actionsSupplier(inlay), toolbarTarget).apply {
      border = this@NotebookCellToolbar.border
    }
    toolbar.transparency = 1f
    inlay.editor.contentComponent.add(toolbar, 0)
    inlay.addComponentListener(this)
    this.toolbar = toolbar
    updateToolbarBounds()
  }

  fun hide() {
    inlay.removeComponentListener(this)

    toolbar?.let { tb ->
      inlay.editor.contentComponent.remove(tb)
      inlay.editor.contentComponent.repaint(tb.bounds)
      toolbar = null
    }
  }

  private fun updateToolbarBounds() {
    when (position) {
      CellToolbarPosition.TOP_RIGHT -> {
        toolbar?.let {
          it.setBounds(inlay.x + inlay.width - it.preferredSize.width - InlayDimensions.rightOffset,
                       inlay.editor.offsetToXY(inlay.cellTextRange.startOffset).y - it.preferredSize.height / 2
                       - (if (inlay.cell.titleVisible) InlayDimensions.lineHeight else 0),
                       it.preferredSize.width,
                       it.preferredSize.height)
        }
      }

      // Below inlay.
      CellToolbarPosition.BOTTOM_CENTER -> {
        toolbar?.let {
          it.setBounds(inlay.x + inlay.width / 2 - it.preferredSize.width / 2,
                       inlay.y + inlay.height - it.preferredSize.height,
                       it.preferredSize.width,
                       it.preferredSize.height)
        }
      }

      // Above cell text.
      CellToolbarPosition.TOP_CENTER -> {
        toolbar?.let {
          it.setBounds(inlay.x + inlay.width / 2 - it.preferredSize.width / 2,
                       inlay.editor.offsetToXY(inlay.cellTextRange.startOffset).y - it.preferredSize.height
                       - (if (inlay.cell.titleVisible) InlayDimensions.lineHeight else 0),
                       it.preferredSize.width,
                       it.preferredSize.height)
        }
      }
    }
  }

  fun dispose() {
    inlay.editor.contentComponent.remove(toolbar)
    toolbar = null
  }

  override fun componentResized(e: ComponentEvent?) = updateToolbarBounds()
  override fun componentMoved(e: ComponentEvent?) = updateToolbarBounds()
  override fun componentShown(e: ComponentEvent?) = Unit
  override fun componentHidden(e: ComponentEvent?) = Unit

  companion object {

    /** Action on the cell bottom for creating new cell below/above. */
    private fun createNewCellAction(inlay: NotebookInlayComponent, actionId: String): AnAction {
      val action = ActionManager.getInstance().getAction(actionId)
      val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(action)

      val button = NotebookCellToolbarButton(VisMessagesBundle.message("toolbar.add"), AllIcons.General.Add)
      button.setShortcutText(shortcutText)

      button.addActionListener {
        action.actionPerformed(NotebookInlayUtils.createActionEvent(inlay.editor, inlay.cell))
      }

      return CustomComponentActionImpl(button)
    }

    /** Returns null for last-bottom and first-top cell. */
    private fun createMergeCellAction(inlay: NotebookInlayComponent, topPositioned: Boolean): AnAction? {
      val note = inlay.cell.note ?: return null
      val indexInNote = inlay.cell.indexInNote

      if (topPositioned && indexInNote == 0 || !topPositioned && indexInNote == note.cells.size - 1) {
        return null
      }

      // Remember that ZeppelinMergeCellsAction actually did mergeCellWithNext.
      val action = ActionManager.getInstance().getAction("ZeppelinMergeCellsAction")
      val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(action)

      val button = NotebookCellToolbarButton(VisMessagesBundle.message("toolbar.merge"), AllIcons.Actions.Collapseall)
      button.setShortcutText(shortcutText)

      button.addActionListener {
        val targetCell = if (topPositioned) note.cells[inlay.cell.indexInNote - 1] else inlay.cell
        action.actionPerformed(NotebookInlayUtils.createActionEvent(inlay.editor, targetCell))
      }

      return CustomComponentActionImpl(button)
    }

    fun createNewCellAboveActions(inlay: NotebookInlayComponent) = DefaultActionGroup(
      createNewCellAction(inlay, "ZeppelinInsertCellAboveAction")).apply {
      createMergeCellAction(inlay, true)?.let { add(it) }
    }

    fun createNewCellBelowActions(inlay: NotebookInlayComponent) = DefaultActionGroup(
      createNewCellAction(inlay, "ZeppelinInsertCellBelowAction")).apply {
      createMergeCellAction(inlay, false)?.let { add(it) }
    }

    fun getTypeAndUrlOfJob(inlay: NotebookInlayComponent): Pair<String, String>? {
      val inlayJson = inlay.cell.asJsonTree()
      val info = if (inlayJson.has("runtimeInfos")) inlayJson["runtimeInfos"] as JsonObject? else null

      val jobUrlProperty = info?.get("jobUrl")?.asJsonObject
      val typeOfJob = jobUrlProperty?.get("group")?.asString ?: ""
      if (typeOfJob != "spark" && typeOfJob != "flink")
        return null
      val jobUrls = jobUrlProperty?.getAsJsonArray("values")
      return if (jobUrls == null || jobUrls.size() == 0) null
      else {
        val jsonElement = jobUrls[0]
        when {
          jsonElement.isJsonObject -> Pair(typeOfJob, jsonElement.asJsonObject["jobUrl"].asString)
          jsonElement.isJsonPrimitive -> Pair(typeOfJob, jsonElement.asString)
          else -> null
        }
      }
    }

    /** Action on the cell top run/clear/stop. */
    fun createCellActions(inlay: NotebookInlayComponent): DefaultActionGroup {

      val actionManager = ActionManager.getInstance()

      val actionClear = actionManager.getAction("ZeppelinClearCellOutput")
      val actionStop = actionManager.getAction("ZeppelinStopCellAction")

      val detailsLabel = ActionLink(VisMessagesBundle.message("toolbar.openJob.link")) {
        val jobUrl = getTypeAndUrlOfJob(inlay)
        val project = inlay.editor.project
        if (jobUrl != null && project != null) {
          JobUrlHelper.openJobUrl(inlay.editor, project, jobUrl.first, jobUrl.second)
        }
      }

      val actionJobUrl = object : CustomComponentActionImpl(detailsLabel) {
        override fun update(e: AnActionEvent) {
          e.presentation.isVisible = getTypeAndUrlOfJob(inlay) != null
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
      }

      val minorActions = DefaultActionGroup(listOf(actionManager.getAction("ZeppelinRunCellAction"),
                                                   actionManager.getAction("ZeppelinRunAllAboveAction"),
                                                   actionManager.getAction("ZeppelinRunAllBelowAction"),
                                                   Separator(),
                                                   actionManager.getAction("ZeppelinMoveCellAboveAction"),
                                                   actionManager.getAction("ZeppelinMoveCellBelowAction"),
                                                   Separator(),
                                                   actionManager.getAction("ZeppelinCloneCellAction"),
                                                   actionManager.getAction("ZeppelinToggleCellTitle"),
                                                   actionManager.getAction("ZeppelinRestartCellAction"),
                                                   Separator(),
                                                   actionManager.getAction("ZeppelinMergeCellsAction"),
                                                   actionManager.getAction("ZeppelinSplitCellAction"),
                                                   Separator(),
                                                   actionManager.getAction("ZeppelinDeleteCellAction"))).apply {
        isPopup = true
        templatePresentation.icon = AllIcons.Actions.More
        templatePresentation.text = VisMessagesBundle.message("actions.more")
        templatePresentation.putClientProperty(ActionUtil.HIDE_DROPDOWN_ICON, true)
      }

      val actions = listOf(actionClear, actionStop, actionJobUrl, minorActions)

      return if (InlaysConfig.getInstance().cellToolbarStyle == InlaysToolbarStyle.FULL) {
        DefaultActionGroup(actions)
      }
      else {
        DefaultActionGroup(HoverPopupAction(null, null, AllIcons.Actions.More, actions))
      }
    }
  }
}