package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.settings.adapters.ChartSettingsZeppelinAdapter
import com.intellij.charts.settings.data.BarSeriesSettings
import com.intellij.charts.settings.data.LineSeriesSettings
import com.intellij.charts.settings.data.PieSeriesSettings
import com.intellij.charts.settings.data.SeriesSettings
import com.intellij.charts.settings.data.type.BarSeriesType
import com.intellij.charts.settings.data.type.LineSeriesType
import com.intellij.charts.settings.data.type.PieSeriesType
import com.intellij.charts.settings.series.BaseSeriesPanel
import com.intellij.charts.utils.getAsJsonObjectOrNull
import com.intellij.charts.utils.optBoolean

/** Settings of single data series. Axes and series type selection. */
class InlayBaseSeriesPanel(dataFrame: DataFrame, private val cell: NotebookCell, settings: SeriesSettings) :
  BaseSeriesPanel(dataFrame, settings) {

  /** Populates given seriesSettings with data from some external source. This is Zeppelin specific code and json field names. */
  override fun populateUniqueSettings(seriesSettings: SeriesSettings) {
    if (seriesSettings.type != LineSeriesType.instance &&
        seriesSettings.type != BarSeriesType.instance &&
        seriesSettings.type != PieSeriesType.instance) {
      return
    }

    val settings = cell.asJsonTree()

    if (settings.isJsonNull) {
      return
    }

    val graph = ChartSettingsZeppelinAdapter.getZeppelinChartSettings(settings) ?: return

    when (seriesSettings) {
      is BarSeriesSettings -> {
        val multiBarChartObject = graph.getAsJsonObjectOrNull("setting")?.getAsJsonObjectOrNull("multiBarChart") ?: return
        seriesSettings.horizontal = multiBarChartObject.optBoolean("horizontal")
        seriesSettings.stacked = multiBarChartObject.optBoolean("stacked")
      }
      is LineSeriesSettings -> {
        val lineSettings = graph.getAsJsonObjectOrNull("setting")?.getAsJsonObjectOrNull("lineChart") ?: return
        seriesSettings.stepped = lineSettings.optBoolean("stepped")
        seriesSettings.showPoints = lineSettings.optBoolean("showPoints")
      }
      is PieSeriesSettings -> {
        val pieSettings = graph.getAsJsonObjectOrNull("setting")?.getAsJsonObjectOrNull("pieChart") ?: return
        seriesSettings.showPercents = pieSettings.optBoolean("showPercents")
      }
    }
  }
}