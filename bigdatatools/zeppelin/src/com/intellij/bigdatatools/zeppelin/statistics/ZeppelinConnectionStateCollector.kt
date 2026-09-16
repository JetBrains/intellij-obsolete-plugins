package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager

class ZeppelinConnectionStateCollector : ProjectUsagesCollector() {

  override fun getGroup() = GROUP

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val connections = RfsConnectionDataManager.instance?.getTyped<ZeppelinConnectionData>(project) ?: emptyList()

    val metrics = connections.map { config: ZeppelinConnectionData ->
      CONFIGURED_EVENT.metric(CONNECTION_SCALA_VERSION.with(ZeppelinAllowedList.libraryVersion(config.scalaVersion)),
                              CONNECTION_SPARK_VERSION.with(ZeppelinAllowedList.libraryVersion(config.sparkVersion)),
                              CONNECTION_HADOOP_VERSION.with(ZeppelinAllowedList.libraryVersion(config.hadoopVersion)),
                              CONNECTION_NGINX_AUTH_ENABLED.with(config.enableBasicAuth),
                              CONNECTION_ANONYMOUS.with(config.anonymous),
                              CONNECTION_PER_PROJECT.with(config.isPerProject),
                              CONNECTION_SECURED.with(config.uri.contains("https")),
                              SERVER_IS_LOCAL.with(config.uri.contains("localhost") || config.uri.contains("127.0.0.1")))
    }
    return metrics.toSet()
  }

  companion object {
    private val GROUP = EventLogGroup("bigdatatools.zeppelin.configurations", 3)

    private val CONNECTION_SCALA_VERSION = EventFields.StringValidatedByRegexpReference("connection_scala_version", "version")
    private val CONNECTION_SPARK_VERSION = EventFields.StringValidatedByRegexpReference("connection_spark_version", "version")
    private val CONNECTION_HADOOP_VERSION = EventFields.StringValidatedByRegexpReference("connection_hadoop_version", "version")
    private val CONNECTION_NGINX_AUTH_ENABLED = EventFields.Boolean("connection_nginx_auth_enabled")
    private val CONNECTION_ANONYMOUS = EventFields.Boolean("connection_anonymous")
    private val CONNECTION_PER_PROJECT = EventFields.Boolean("connection_perProject")
    private val CONNECTION_SECURED = EventFields.Boolean("connection_secured")
    private val SERVER_IS_LOCAL = EventFields.Boolean("server_is_local")

    private val CONFIGURED_EVENT = GROUP.registerVarargEvent("connection.configured",
                                                             CONNECTION_SCALA_VERSION,
                                                             CONNECTION_SPARK_VERSION,
                                                             CONNECTION_HADOOP_VERSION,
                                                             CONNECTION_NGINX_AUTH_ENABLED,
                                                             CONNECTION_ANONYMOUS,
                                                             CONNECTION_PER_PROJECT,
                                                             CONNECTION_SECURED,
                                                             SERVER_IS_LOCAL)
  }
}