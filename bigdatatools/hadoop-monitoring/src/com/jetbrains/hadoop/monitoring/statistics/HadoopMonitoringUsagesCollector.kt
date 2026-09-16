package com.jetbrains.hadoop.monitoring.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.RoundedIntEventField
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

object HadoopMonitoringUsagesCollector : CounterUsagesCollector() {

  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("bigdatatools.hadoop.monitoring", 2)

  private val secureIntEventField = RoundedIntEventField("count")

  val appsReceivedEvent = GROUP.registerEvent("apps.received", secureIntEventField)
  val nodesReceivedEvent = GROUP.registerEvent("nodes.received", secureIntEventField)
  val nodesLabelsReceivedEvent = GROUP.registerEvent("node.labels.received", secureIntEventField)
  val appAttemptsReceivedEvent = GROUP.registerEvent("app.attempts.received", secureIntEventField)
  val logFileOpenedEvent = GROUP.registerEvent("log.file.opened", RoundedIntEventField("size"))
  val logListReceivedEvent = GROUP.registerEvent("log.list.opened", secureIntEventField)
  val openedInBrowserEvent = GROUP.registerEvent("opened.in.browser")
  val openedInSeparateTabEvent = GROUP.registerEvent("opened.in.separate.tab")
  val killEvent = GROUP.registerEvent("app.kill")
}