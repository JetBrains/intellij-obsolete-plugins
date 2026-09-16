package com.jetbrains.bigdatatools.glue.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.glue.icons.BigdatatoolsGlueIcons
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.MonitoringConnectionGroup

class GlueConnectionGroup : ConnectionFactory<GlueConnectionData>(
  id = BdtConnectionType.GLUE.id,
  name = BdtConnectionType.GLUE.connName,
  icon = BigdatatoolsGlueIcons.AwsGlue,
  parentGroupId = MonitoringConnectionGroup.GROUP_ID
) {
  override fun newData() = GlueConnectionData().apply {
    name = BdtConnectionType.GLUE.connName
    uri = "thrift://127.0.0.1:9083"
  }
}