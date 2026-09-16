package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.charts.core.settings.ChartSettings
import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.settings.SeriesSettingsPanel
import com.intellij.charts.settings.data.SeriesSettings

/** Main series setup panel show on the ToolWindow. */
class InlaySeriesSettingsPanel(dataFrame: DataFrame, private val cell: NotebookCell, chartSettings: ChartSettings, mode: Mode)
  : SeriesSettingsPanel(dataFrame, chartSettings, mode) {

  override fun createSeriesPanel(settings: SeriesSettings) = InlayBaseSeriesPanel(dataFrame, cell, settings)
}