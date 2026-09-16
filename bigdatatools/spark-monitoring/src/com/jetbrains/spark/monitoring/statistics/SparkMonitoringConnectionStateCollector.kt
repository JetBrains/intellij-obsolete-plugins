package com.jetbrains.spark.monitoring.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.BooleanEventField
import com.intellij.internal.statistic.eventLog.events.EnumEventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.RfsConnectionStateCollector
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.spark.monitoring.settings.SparkConnectionData

class SparkMonitoringConnectionStateCollector : ProjectUsagesCollector() {

  override fun getGroup(): EventLogGroup = Util.GROUP

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val connections = RfsConnectionDataManager.instance?.getTyped<SparkConnectionData>(project)

    val metrics = connections?.map { config: SparkConnectionData ->
      Util.EVENT.metric(Util.DRIVER_TYPE.with(config.rfsDriverType()),
                        EventFields.Enabled.with(config.isEnabled),
                        RfsConnectionStateCollector.PER_PROJECT.with(config.isPerProject),
                        RfsConnectionStateCollector.BASIC_AUTH_ENABLED.with(config.enableBasicAuth),
                        Util.HISTORY_SERVER.with(config.historyServer),
                        RfsConnectionStateCollector.PROXY_TYPE.with(config.proxyEnableType),
                        RfsConnectionStateCollector.CONNECTION_SECURED.with(config.uri.contains("https")),
                        RfsConnectionStateCollector.SERVER_IS_LOCAL.with(
                          config.uri.contains("localhost") || config.uri.contains("127.0.0.1")))
    }

    return metrics?.toSet() ?: setOf()
  }

  object Util {
    val GROUP: EventLogGroup = EventLogGroup("bigdatatools.spark.monitoring.configurations", 9)
    val DRIVER_TYPE: EnumEventField<BdtConnectionType> = EventFields.Enum<BdtConnectionType>("driver_type")
    val HISTORY_SERVER: BooleanEventField = EventFields.Boolean("history_server")
    val EVENT: VarargEventId = GROUP.registerVarargEvent("connection.configured",
                                               DRIVER_TYPE,
                                               EventFields.Enabled,
                                               RfsConnectionStateCollector.PER_PROJECT,
                                               RfsConnectionStateCollector.BASIC_AUTH_ENABLED,
                                               HISTORY_SERVER,
                                               RfsConnectionStateCollector.PROXY_TYPE,
                                               RfsConnectionStateCollector.CONNECTION_SECURED,
                                               RfsConnectionStateCollector.SERVER_IS_LOCAL)
  }
}