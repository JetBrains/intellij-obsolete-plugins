package com.intellij.dataWrangler.core.statistics.action

import com.intellij.dataWrangler.core.CoreDataWranglerBundle
import com.intellij.dataWrangler.core.statistics.CoreStatisticsHeader
import com.intellij.dataWrangler.core.statistics.findCoreStatisticsHeader
import com.intellij.database.DatabaseDataKeys
import com.intellij.database.run.ui.table.TableResultView
import com.intellij.database.run.ui.table.statisticsPanel.StatisticsPanelMode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

sealed class CoreStatisticsDisplayAction(private val presentationMode: StatisticsPanelMode) : ToggleAction(), DumbAware {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  override fun update(e: AnActionEvent) {
    e.presentation.keepPopupOnPerform = KeepPopupOnPerform.Never
    e.presentation.isEnabledAndVisible = isStatisticsAvailable(e)
    super.update(e)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    val grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY) ?: return false
    val tableWithStatistics = grid.resultView as? TableResultView ?: return false

    return tableWithStatistics.getStatisticsPanelMode() == presentationMode
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY) ?: return
    val tableWithStatistics = grid.resultView as? TableResultView ?: return

    val header = findCoreStatisticsHeader(e.dataContext) ?: CoreStatisticsHeader(tableWithStatistics, grid)
    header.setMode(presentationMode)
  }

  class StatisticsShowOff : CoreStatisticsDisplayAction(StatisticsPanelMode.OFF)

  class StatisticsShowCompact : CoreStatisticsDisplayAction(StatisticsPanelMode.COMPACT)

  class StatisticsShowDetailed : CoreStatisticsDisplayAction(StatisticsPanelMode.DETAILED)
}

class CoreStatisticsDisplayActionGroup : DefaultActionGroup(), DumbAware {
  init {
    addSeparator(CoreDataWranglerBundle.message("action.core.Console.StatisticsShow.separator"))
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isStatisticsAvailable(e)
  }
}

private fun isStatisticsAvailable(@Suppress("UNUSED_PARAMETER") e: AnActionEvent): Boolean {
  return false
}
