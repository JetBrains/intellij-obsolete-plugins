package com.jetbrains.bigdatatools.hivemetastore.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.hiveMetastore.icons.BigdatatoolsHiveMetastoreIcons
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.MonitoringConnectionGroup

class HiveMetastoreConnectionGroup : ConnectionFactory<HiveMetastoreConnectionData>(
  id = BdtConnectionType.HIVE.id,
  name = BdtConnectionType.HIVE.connName,
  icon = BigdatatoolsHiveMetastoreIcons.Apache_hive,
  parentGroupId = MonitoringConnectionGroup.GROUP_ID
) {
  override fun newData() = HiveMetastoreConnectionData().apply {
    name = BdtConnectionType.HIVE.connName
    uri = "thrift://127.0.0.1:9083"
  }
}