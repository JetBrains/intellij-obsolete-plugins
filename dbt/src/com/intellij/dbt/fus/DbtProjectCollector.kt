package com.intellij.dbt.fus

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventId
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

internal object DbtProjectCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  private val GROUP: EventLogGroup = EventLogGroup("dbt.framework", 1)

  private val DBT_PROJECT_INITIALIZED_EVENT: EventId = GROUP.registerEvent("dbt.project.initialized")

  fun logDbtProjectInitialized() {
    DBT_PROJECT_INITIALIZED_EVENT.log()
  }
}