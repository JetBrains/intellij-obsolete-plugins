package com.intellij.dataWrangler.core.statistics

import com.intellij.dataWrangler.core.statistics.model.CoreStatisticsHeaderDataProvider
import com.intellij.dataWrangler.core.statistics.view.CoreStatisticsTableHeaderPanel
import com.intellij.database.DatabaseDataKeys.DATA_GRID_KEY
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.run.ui.table.TableResultView
import com.intellij.database.run.ui.table.statisticsPanel.StatisticsPanelMode
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.util.Key

internal class CoreStatisticsHeader(
  val table: TableResultView,
  grid: DataGrid,
) {
  val dataProvider: CoreStatisticsHeaderDataProvider = CoreStatisticsHeaderDataProvider(grid)
  val headerPanel: CoreStatisticsTableHeaderPanel = CoreStatisticsTableHeaderPanel(table, dataProvider, grid.coroutineScope)

  init {
    table.statisticsHeader = headerPanel
    grid.putUserData(CORE_STATISTICS_HEADER_KEY, this)
  }

  fun setMode(mode: StatisticsPanelMode) {
    table.statisticsPanelMode = mode
  }
}

@JvmField
internal val CORE_STATISTICS_HEADER_DATA_KEY: DataKey<CoreStatisticsHeader> = DataKey.create("CORE_STATISTICS_HEADER_DATA_KEY")

@JvmField
internal val CORE_STATISTICS_HEADER_KEY: Key<CoreStatisticsHeader> = Key("CORE_STATISTICS_HEADER_KEY")

internal fun findCoreStatisticsHeader(context: DataContext): CoreStatisticsHeader? {
  val view = context.getData(CORE_STATISTICS_HEADER_DATA_KEY)
  if (view != null) return view

  val grid = context.getData(DATA_GRID_KEY) ?: return null

  return grid.getUserData(CORE_STATISTICS_HEADER_KEY)
}