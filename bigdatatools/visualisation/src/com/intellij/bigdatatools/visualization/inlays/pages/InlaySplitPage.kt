package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.visualization.inlays.InlayDimensions
import com.intellij.bigdatatools.visualization.inlays.components.ChildrenHidePanel
import com.intellij.bigdatatools.visualization.inlays.components.ToolbarVisibility
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.inlays.style.OutputToolbarPosition
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.charts.core.CHART_PAGE_DATA_KEY
import com.intellij.charts.settings.adapters.ChartSettingsZeppelinAdapter
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsActions
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import icons.ChartsIcons
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max

/** Page for displaying Zeppelin pair of table and chart. */
internal class InlaySplitPage(val tablePage: InlayTablePage,
                              val chartPage: InlayChartPage,
                              private val console: InlayTextPage? = null) : InlayPage, ToolbarVisibility {

  enum class Mode {
    TABLE,
    CHART,
    CONSOLE,
    SPLIT
  }

  override var indexInResults = -1

  override val component: OnePixelSplitter = object : OnePixelSplitter(false), UiDataProvider {
    override fun uiDataSnapshot(sink: DataSink) {
      sink[NOTEBOOK_INLAY_SPLIT_PAGE_KEY] = this@InlaySplitPage
      sink[CHART_PAGE_DATA_KEY] = chartPage
    }
  }.apply { isOpaque = false }

  override val title
    get() = VisMessagesBundle.message("page.title.split")

  override val contentType = InlayPageContentType.MIXED

  var mode = Mode.CHART
    private set

  private var toolbarPanels = mutableListOf<ToolbarVisibility>()

  private val tablePanel = createPanel(tablePage)

  private val chartPanel = createPanel(chartPage)

  private val consolePanel = if (console == null) null else createPanel(console)

  override var showToolbar = false
    set(value) {
      field = value
      toolbarPanels.forEach { it.showToolbar = value }
    }

  init {
    if (chartPage.isEmpty || tablePage.isDefault) {
      mode = Mode.TABLE
    }

    Disposer.register(this, tablePage)
    Disposer.register(this, chartPage)
    console?.let { Disposer.register(this, it) }

    switchMode(mode)

    // ToDo OnePixelSplitter overrides preferredSize and calculates it basing on first and second Component. This code only to trigger event.
    component.preferredSize = Dimension(component.preferredSize.width,
                                        max(tablePage.component.preferredSize.height, InlayDimensions.defaultHeight))
  }

  override fun getCollapsedDescription() = tablePage.getCollapsedDescription()

  private fun createSwitchToAction(targetMode: Mode): AnAction = object : DumbAwareToggleAction(getActionNameForMode(targetMode), null,
                                                                                                getActionIconForMode(targetMode)) {
    override fun isSelected(e: AnActionEvent) = mode == targetMode
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun setSelected(e: AnActionEvent, state: Boolean) {
      if (state) {
        switchMode(targetMode)
        if (targetMode == Mode.CHART) {
          val firstSeriesType = chartPage.getSettings().seriesSettings.firstOrNull()?.type
          val zeppelinSeriesType = if (firstSeriesType == null) null
          else ChartSettingsZeppelinAdapter.seriesTypeToZeppelinName(firstSeriesType)
          tablePage.tabChanged(zeppelinSeriesType ?: "lineChart")
        }
        else if (targetMode == Mode.TABLE) {
          tablePage.tabChanged("table")
        }
      }
    }
  }

  private fun createToolbarPanel(actions: List<AnAction>): JComponent {
    val toolbarPanel = ChildrenHidePanel().apply {

      val actionToolbar = ToolbarUtils.createActionToolbar(component, "BDTTableAndChart", actions, isToolbarHorizontal())

      val toolbar = actionToolbar.component.apply {
        isOpaque = !InlaysConfig.getInstance().transparentOutput
      }

      add(toolbar)
      showToolbar = this@InlaySplitPage.showToolbar
    }
    toolbarPanels.add(toolbarPanel)
    return toolbarPanel
  }

  private fun createPanel(page: InlayPage): JPanel {
    val actions = Mode.entries.mapNotNull {
      if (it != Mode.CONSOLE || console != null) createSwitchToAction(it)
      else null
    } + Separator() + page.createActions()

    return JPanel(BorderLayout()).apply {
      isOpaque = false
      add(page.component, BorderLayout.CENTER)
      preferredSize = page.component.preferredSize
    }
  }

  fun supportsMode(mode: Mode): Boolean {
    return mode != Mode.CONSOLE || console != null
  }

  fun switchMode(mode: Mode) {
    when (mode) {
      Mode.CHART -> {
        component.firstComponent = tablePanel
        tablePanel.isVisible = false
        component.secondComponent = chartPanel
        chartPanel.isVisible = true
      }
      Mode.TABLE -> {
        component.firstComponent = tablePanel
        tablePanel.isVisible = true
        component.secondComponent = chartPanel
        chartPanel.isVisible = false
      }
      Mode.SPLIT -> {
        component.firstComponent = tablePanel
        tablePanel.isVisible = true
        component.secondComponent = chartPanel
        chartPanel.isVisible = true
      }
      Mode.CONSOLE -> {
        component.firstComponent = consolePanel
        component.secondComponent?.isVisible = false
      }
    }
    this.mode = mode
  }

  companion object {
    val NOTEBOOK_INLAY_SPLIT_PAGE_KEY = DataKey.create<InlaySplitPage>("NOTEBOOK_INLAY_SPLIT_PAGE_KEY")

    private fun getActionIconForMode(mode: Mode) = when (mode) {
      Mode.TABLE -> AllIcons.Nodes.DataTables
      Mode.CHART -> ChartsIcons.Chart.ChartArea
      Mode.CONSOLE -> AllIcons.Nodes.Console
      Mode.SPLIT -> AllIcons.Actions.SplitVertically
    }

    @NlsActions.ActionText
    private fun getActionNameForMode(mode: Mode) = when (mode) {
      Mode.CHART -> VisMessagesBundle.message("action.BDI.Notebook.ToggleChartInlayView.text")
      Mode.TABLE -> VisMessagesBundle.message("action.BDI.Notebook.ToggleTableInlayView.text")
      Mode.CONSOLE -> VisMessagesBundle.message("action.BDI.Notebook.ToggleConsoleInlayView.text")
      Mode.SPLIT -> VisMessagesBundle.message("action.BDI.Notebook.ToggleSplitInlayView.text")
    }

    fun getToolbarPosition() = when (InlaysConfig.getInstance().outputToolbarPosition) {
      OutputToolbarPosition.TOP -> BorderLayout.NORTH
      OutputToolbarPosition.LEFT -> BorderLayout.WEST
      OutputToolbarPosition.RIGHT -> BorderLayout.EAST
      OutputToolbarPosition.BOTTOM -> BorderLayout.SOUTH
    }

    private fun isToolbarHorizontal() = InlaysConfig.getInstance().outputToolbarPosition != OutputToolbarPosition.LEFT &&
                                        InlaysConfig.getInstance().outputToolbarPosition != OutputToolbarPosition.RIGHT
  }
}