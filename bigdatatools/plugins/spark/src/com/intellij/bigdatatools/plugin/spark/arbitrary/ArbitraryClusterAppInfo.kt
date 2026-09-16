package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtAppInfo

data class ArbitraryClusterAppInfo(override val connType: BdtConnectionType?) : BdtAppInfo {
  override val name: String = connType?.connName ?: ""
  override val url: String = ""
  override val possibleOpenInBrowser: Boolean = false
}