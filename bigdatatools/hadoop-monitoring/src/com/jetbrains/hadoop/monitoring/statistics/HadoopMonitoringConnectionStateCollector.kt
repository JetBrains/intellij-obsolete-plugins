package com.jetbrains.hadoop.monitoring.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.statistics.RfsConnectionStateCollector
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData

internal class HadoopMonitoringConnectionStateCollector : ProjectUsagesCollector() {
  override fun getGroup() = GROUP

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val connections = RfsConnectionDataManager.instance?.getTyped<HadoopConnectionData>(project)

    val metrics = connections?.map { config: HadoopConnectionData ->
      EVENT.metric(EventFields.Enabled.with(config.isEnabled),
                   RfsConnectionStateCollector.PER_PROJECT.with(config.isPerProject),
                   RfsConnectionStateCollector.BASIC_AUTH_ENABLED.with(config.enableBasicAuth),
                   RfsConnectionStateCollector.PROXY_TYPE.with(config.proxyEnableType),
                   RfsConnectionStateCollector.CONNECTION_SECURED.with(config.uri.contains("https")),
                   RfsConnectionStateCollector.SERVER_IS_LOCAL.with(config.uri.contains("localhost") || config.uri.contains("127.0.0.1")))
    }

    return metrics?.toSet() ?: setOf()
  }

  private val GROUP = EventLogGroup("bigdatatools.hadoop.monitoring.configurations", 2)
  private val EVENT = GROUP.registerVarargEvent("connection.configured",
                                                EventFields.Enabled,
                                                RfsConnectionStateCollector.PER_PROJECT,
                                                RfsConnectionStateCollector.BASIC_AUTH_ENABLED,
                                                RfsConnectionStateCollector.PROXY_TYPE,
                                                RfsConnectionStateCollector.CONNECTION_SECURED,
                                                RfsConnectionStateCollector.SERVER_IS_LOCAL)

}