package com.intellij.dataWrangler.impl.view

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.DWTableDataViewerImpl
import com.intellij.dataWrangler.impl.DataWranglerBundle
import com.intellij.dataWrangler.impl.DataWranglerUiSessionImpl
import com.intellij.dataWrangler.impl.service.DataWranglerSessionImpl
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.dataWrangler.impl.view.steps.TransformationStepsPanel
import com.intellij.dataWrangler.impl.view.transformation.TransformationsPanel
import com.intellij.database.DatabaseDataKeys.DATA_GRID_KEY
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridPanel.ViewPosition
import com.intellij.database.datagrid.RemovableView
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.util.addComponent
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

private val minimalLength = JBUI.scale(200)

class DWMainPanel(
  grid: DataGrid,
  backendSession: DataWranglerSessionImpl<*>,
  showAfterCreation: Boolean = true,
) : SimpleToolWindowPanel(true, true), RemovableView {
  private val session: DataWranglerUiSession<*> = DataWranglerUiSessionImpl(backendSession, DWTableDataViewerImpl(grid))
  val backendSession: DataWranglerSessionImpl<*>
    get() = session.backendSession as DataWranglerSessionImpl<*>
  private val splitter: OnePixelSplitter = OnePixelSplitter(true, .66f)

  override fun uiDataSnapshot(sink: DataSink) {
    super.uiDataSnapshot(sink)
    sink[DATA_WRANGLER_KEY] = this@DWMainPanel
    sink[DW_SESSION] = session.backendSession
  }

  init {
    Disposer.register(grid, session)
    Disposer.register(session) { backendSession.dispose() }
    val main = JPanel(BorderLayout())
    val panelHeader: JPanel = createDWPanelHeader()
    main.add(panelHeader, BorderLayout.NORTH)
    minimumSize = Dimension(minimalLength, minimalLength)

    splitter.divider.setBackground(OnePixelDivider.BACKGROUND)
    createAndAddChildPanels(session)

    main.add(splitter)

    setContent(main)
    addPanel(showAfterCreation)
  }

  // wildcard-capturing, so session and TransformationsPanel both have the same C type for TransformationStepsPanel
  private fun <C : DataWranglerContext> createAndAddChildPanels(session: DataWranglerUiSession<C>) {
    val transformationsPanel = TransformationsPanel(session)
    val transformationStepsPanel = TransformationStepsPanel(session, transformationsPanel)
    splitter.addComponent(transformationsPanel)
    splitter.addComponent(transformationStepsPanel)
  }

  private fun addPanel(show: Boolean) {
    val grid = session.tableViewer.getGrid()
    grid.findDWPanel()?.disposePanel()
    grid.putUserData(DATA_WRANGLER_GRID_KEY, this)
    if (show) {
      showPanel()
    }
  }

  fun showPanel() {
    session.tableViewer.getGrid().panel.putSideView(this, ViewPosition.RIGHT, null)
    requestFocus()
  }

  fun disposePanel() {
    hidePanel()
    session.tableViewer.getGrid().putUserData(DATA_WRANGLER_GRID_KEY, null)
    Disposer.dispose(session)
  }

  fun hidePanel() {
    val grid = session.tableViewer.getGrid()
    session.tableViewer.dropColumnHighlights()
    grid.panel.removeSideView(this)
  }

  override val viewComponent: JComponent?
    get() = component

  override fun onRemoved() {}

  private fun createDWPanelHeader(): JPanel {
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction("DataWrangler.Hide.Panel")
    val toolbar = actionManager.createActionToolbar("TransformToolbar", DefaultActionGroup(action), true).apply {
      component.border = IdeBorderFactory.createEmptyBorder(JBUI.insets(DWProperties.GAP_WIDTH))
      @Suppress("removal", "DEPRECATION")
      setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY)
    }
    toolbar.targetComponent = viewComponent

    val topPanelName = "<html><b>${DataWranglerBundle.message("DataWrangler.main.panel.name")}</b></html>"
    val topPanelLabel = JBLabel(topPanelName).apply {
      border = IdeBorderFactory.createEmptyBorder(JBUI.insets(DWProperties.GAP_WIDTH))

    }

    val topPanel: JPanel = JPanel(BorderLayout()).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
    }

    return topPanel
      .apply {
        add(toolbar.component, BorderLayout.EAST)
        add(topPanelLabel, BorderLayout.WEST)
      }
  }

  companion object {
    fun createPanel(
      @NlsContexts.Label panelTitle: String,
      toolbarPlaceName: String,
      actionGroup: ActionGroup,
    ): JPanel {

      val topPanelTitle = JBLabel(panelTitle).apply {
        foreground = JBColor.GRAY
      }
      val topPanel = JPanel(BorderLayout()).apply {
        border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS)
      }

      val toolbar = ActionManager
        .getInstance()
        .createActionToolbar(toolbarPlaceName, actionGroup, true).apply {
          component.border = null
          @Suppress("removal", "DEPRECATION")
          setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY)
        }

      topPanel.add(topPanelTitle, BorderLayout.WEST)
      toolbar.targetComponent = topPanel
      topPanel.add(toolbar.component, BorderLayout.EAST)
      return topPanel
    }
  }
}

@JvmField
internal val DATA_WRANGLER_KEY: DataKey<DWMainPanel> = DataKey.create("DATA_WRANGLER_KEY")

@JvmField
val DATA_WRANGLER_GRID_KEY: Key<DWMainPanel> = Key("DATA_WRANGLER_GRID_KEY")

fun findDWPanel(context: DataContext): DWMainPanel? {
  return context.getData(DATA_WRANGLER_KEY)
         ?: context.getData(DATA_GRID_KEY)?.findDWPanel()
}

fun DataGrid.findDWPanel(): DWMainPanel? = getUserData(DATA_WRANGLER_GRID_KEY)