package com.jetbrains.hadoop.monitoring.util

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringOpenOptions
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData

object HadoopUtils {
  fun openSparkUrl(project: Project, url: String, connectionData: HadoopConnectionData) {
    val openOptions = MonitoringOpenOptions(tunnelData = connectionData.getTunnelData(),
                                            isPerProject = connectionData.isPerProject,
                                            rememberConnection = {
                                              connectionData.sparkMonitoringDriverId = it
                                            })
    MonitoringServiceProvider.openJobUrl(BdtConnectionType.SPARK_MONITORING.id, project, url, connectionData.sparkMonitoringDriverId,
                                         openOptions)
  }
}