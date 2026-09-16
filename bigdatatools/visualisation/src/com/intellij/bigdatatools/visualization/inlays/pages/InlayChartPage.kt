package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.visualization.inlays.components.InlaySeriesSettingsPanel
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.charts.core.ChartPage
import com.intellij.charts.core.settings.ChartSettings
import com.intellij.charts.settings.SeriesSettingsPanel
import com.intellij.openapi.actionSystem.AnAction
import java.awt.BorderLayout

/** Table page of notebook inlay component. Holds and serves charts. */
class InlayChartPage(private val cell: NotebookCell, chartSettings: ChartSettings, private val mode: SeriesSettingsPanel.Mode) :
  ChartPage(chartSettings, mode), InlayPage {

  override var indexInResults = -1

  override val title
    get() = VisMessagesBundle.message("page.title.chart")

  override val contentType = InlayPageContentType.CHART

  init {
    addToolbar(BorderLayout.EAST)
  }

  override fun dispose() = Unit
  override fun createActions(): List<AnAction> = super<ChartPage>.createActions()

  override fun createSeriesSettingsPanel() = InlaySeriesSettingsPanel(dataFrame, cell, getSettings(), mode).apply { init() }
}