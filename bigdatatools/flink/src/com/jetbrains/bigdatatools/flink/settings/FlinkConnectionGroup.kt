package com.jetbrains.bigdatatools.flink.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.flink.icons.BigdatatoolsFlinkIcons
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.MonitoringConnectionGroup
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData

class FlinkConnectionGroup : ConnectionFactory<FlinkConnectionData>(
  id = BdtConnectionType.FLINK.id,
  name = BdtConnectionType.FLINK.connName,
  icon = BigdatatoolsFlinkIcons.Flink,
  parentGroupId = MonitoringConnectionGroup.GROUP_ID
) {
  override fun newData() = FlinkConnectionData().apply {
    name = BdtConnectionType.FLINK.connName
    uri = "127.0.0.1:8080"
  }
}