package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtSlaveConnection

data class ArbitraryClusterSlaveConnection(override val connectionId: String = "",
                                           override val connectionType: BdtConnectionType) : BdtSlaveConnection {
  override val clusterId: String get() = ArbitraryClusterConnectionData.SINGLE_CLUSTER_ID
}