package com.jetbrains.spark.monitoring.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.spark.monitoring.data.ApplicationStatus
import com.jetbrains.spark.monitoring.data.JobExecutionStatus
import com.jetbrains.spark.monitoring.data.SqlInfoStatus
import com.jetbrains.spark.monitoring.data.StageStatus
import com.jetbrains.spark.monitoring.util.SMMessagesBundle

enum class ToolbarActionType {
  OpenUrl,
  OpenSettings,
  CreateConnection,
  Refresh
}

class SparkMonitoringUsagesCollector : CounterUsagesCollector() {
  override fun getGroup() = GROUP

  companion object {
    enum class Pages(@NlsContexts.TabTitle val text: String) {
      SQL(SMMessagesBundle.message("applications.tab.sql")),
      STORAGES(SMMessagesBundle.message("applications.tab.storage")),
      EXECUTORS(SMMessagesBundle.message("applications.tab.executors")),
      ENVIRONMENT(SMMessagesBundle.message("applications.tab.environment")),
      STAGES(SMMessagesBundle.message("applications.tab.stages")),
      JOBS(SMMessagesBundle.message("applications.tab.jobs")),
      UNKNOWN("");

      companion object {
        fun get(@NlsContexts.TabTitle text: String?): Pages {
          return if (text.isNullOrBlank()) UNKNOWN else values().firstOrNull { text == it.text } ?: UNKNOWN
        }
      }
    }

    enum class NumberIntervals(val text: String) {
      INTERVAL_0("0"),
      INTERVAL_1("1"),
      INTERVAL_2_5("2_5"),
      INTERVAL_6_15("6-15"),
      INTERVAL_16_30("16_30"),
      INTERVAL_31_MORE("31_more"),
    }

    //Possible output is: 0, 1, 2_5, 6_15, 16_30, 31_more
    private fun numberToInterval(number: Int): NumberIntervals {
      return when (number) {
        0 -> NumberIntervals.INTERVAL_0
        1 -> NumberIntervals.INTERVAL_1
        in 2..5 -> NumberIntervals.INTERVAL_2_5
        in 6..15 -> NumberIntervals.INTERVAL_6_15
        in 16..30 -> NumberIntervals.INTERVAL_16_30
        else -> NumberIntervals.INTERVAL_31_MORE
      }
    }
    private val GROUP = EventLogGroup("bigdatatools.spark.monitoring", 4)

    /** Connected: running applications count (0, 1, 2_5, 6_15, 16_30, 31_more).  */
    private val connectedEvent = GROUP.registerEvent("connected",
                                                     EventFields.Enum<NumberIntervals>("total_applications"),
                                                     EventFields.Enum<NumberIntervals>("running_applications"))

    fun logCollectedEvent(project: Project, applicationCount: Int, runningApplicationsCount: Int) {
      connectedEvent.log(project,
                         numberToInterval(applicationCount),
                         numberToInterval(runningApplicationsCount))
    }

    /**  Action invoked: action type (select tab, select item, expand item, collapse item), item type, tab. */
    private val pageSelectedEvent = GROUP.registerEvent("page.selected",
                                                        EventFields.Enum<Pages>("name"))

    fun logPageSelectedEvent(project: Project, @NlsContexts.TabTitle pageName: String) {
      pageSelectedEvent.log(project, Pages.get(pageName))
    }

    val panelExpandedEvent = GROUP.registerEvent("panel.expanded",
                                                 EventFields.Enum<UIPanelType>("name"),
                                                 EventFields.Boolean("value"))

    val blockShownEvent = GROUP.registerEvent("block.shown",
                                              EventFields.Enum<StageUIBlockType>("name"),
                                              EventFields.Boolean("value"))

    val toolbarActionInvokedEvent = GROUP.registerEvent("toolbar.action.invoked",
                                                        EventFields.Enum<ToolbarActionType>("type"))

    val columnVisibilityChangedEvent = GROUP.registerEvent("columns.visibility.changed",
                                                           EventFields.Enum<TableType>("type"),
                                                           EventFields.Boolean("value"))

    val stateFilterChangedEvent = GROUP.registerEvent("state.filter.changed",
                                                      EventFields.Enum<StateFilerType>("type"),
                                                      EventFields.String("field", ApplicationStatus.entries.map { it.text } +
                                                                                  JobExecutionStatus.entries.map { it.text } +
                                                                                  SqlInfoStatus.entries.map { it.text } +
                                                                                  StageStatus.entries.map { it.text }),
                                                      EventFields.Boolean("value"))
  }
}