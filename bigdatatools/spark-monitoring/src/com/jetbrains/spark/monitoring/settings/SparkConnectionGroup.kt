package com.jetbrains.spark.monitoring.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.MonitoringConnectionGroup

class SparkConnectionGroup : ConnectionFactory<SparkConnectionData>(
  id = BdtConnectionType.SPARK_MONITORING.id,
  name = BdtConnectionType.SPARK_MONITORING.connName,
  icon = BigdatatoolsSparkMonitoringIcons.Spark,
  parentGroupId = MonitoringConnectionGroup.GROUP_ID
) {
  override fun newData() = SparkConnectionData().apply {
    name = BdtConnectionType.SPARK_MONITORING.connName
    uri = "localhost:4040"
  }
}