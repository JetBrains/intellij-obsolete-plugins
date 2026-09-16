package com.intellij.bigdatatools.databricks.settings

import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.DataProcessingPlatformConnectionGroup

class DatabricksConnectionGroup : ConnectionFactory<DatabricksConnectionData>(
  id = BdtConnectionType.DATABRICKS.id,
  name = BdtConnectionType.DATABRICKS.connName,
  icon = BigdatatoolsDatabricksIcons.Databricks,
  parentGroupId = DataProcessingPlatformConnectionGroup.GROUP_ID
) {
  override fun newData() = DatabricksConnectionData().apply {
    name = BdtConnectionType.DATABRICKS.connName
  }
}