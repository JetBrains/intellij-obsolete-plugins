package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.IntEventField
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

object ZeppelinInstanceUsageCollector : CounterUsagesCollector() {

  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("bigdatatools.zeppelin.instance", 5)

  val connectedEvent = GROUP.registerEvent("connect", zeppelin_version)

  val foldersUpdateEvent = GROUP.registerEvent("notebook.list.update", zeppelin_version, IntEventField("notebooks_count"))
}