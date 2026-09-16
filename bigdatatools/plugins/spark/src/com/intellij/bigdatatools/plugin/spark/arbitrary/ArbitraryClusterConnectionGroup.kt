package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.DataProcessingPlatformConnectionGroup

class ArbitraryClusterConnectionGroup : ConnectionFactory<ArbitraryClusterConnectionData>(
  id = BdtConnectionType.ARBITRARY_CLUSTER.id,
  name = BdtConnectionType.ARBITRARY_CLUSTER.connName,
  icon = BigdatatoolsPluginSparkIcons.ArbitraryCluster,
  parentGroupId = DataProcessingPlatformConnectionGroup.GROUP_ID
) {
  override fun newData() = ArbitraryClusterConnectionData().apply {
    name = BdtConnectionType.ARBITRARY_CLUSTER.connName
  }
}