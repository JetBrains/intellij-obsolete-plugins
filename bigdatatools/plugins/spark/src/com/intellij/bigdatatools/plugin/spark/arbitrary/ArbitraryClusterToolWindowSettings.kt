package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings

@Service
@State(name = "ArbitraryClusterSettings", storages = [Storage("bdt-arbitrary-cluster.xml")])
class ArbitraryClusterToolWindowSettings : PersistentStateComponent<ArbitraryClusterToolWindowSettings>, IntervalUpdateSettings {
  override var dataUpdateIntervalMillis: Int = 30000
  override var selectedConnectionId: String? = null

  override val configs: MutableMap<String, ArbitraryClusterConfig> = mutableMapOf()


  override fun getState(): ArbitraryClusterToolWindowSettings = this

  override fun loadState(state: ArbitraryClusterToolWindowSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    fun getInstance(): ArbitraryClusterToolWindowSettings = service()
  }
}