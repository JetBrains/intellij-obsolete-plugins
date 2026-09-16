package com.jetbrains.hadoop.monitoring.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.hadoopMonitoring.icons.BigdatatoolsHadoopMonitoringIcons
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.MonitoringConnectionGroup

class HadoopConnectionGroup : ConnectionFactory<HadoopConnectionData>(
  id = BdtConnectionType.YARN.id,
  name = BdtConnectionType.YARN.connName,
  icon = BigdatatoolsHadoopMonitoringIcons.ToolWindowHadoop,
  parentGroupId = MonitoringConnectionGroup.GROUP_ID
) {
  override fun newData() = HadoopConnectionData().apply {
    name = BdtConnectionType.YARN.connName
    uri = "http://localhost:8088"
  }
}