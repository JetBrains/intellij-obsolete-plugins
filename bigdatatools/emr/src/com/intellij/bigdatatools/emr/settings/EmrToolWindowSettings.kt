package com.intellij.bigdatatools.emr.settings

import com.intellij.bigdatatools.emr.model.EmrClusterAppInfo
import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.model.EmrClusterInstanceInfo
import com.intellij.bigdatatools.emr.model.EmrClusterState
import com.intellij.bigdatatools.emr.model.EmrClusterStepInfo
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import software.amazon.awssdk.services.emr.model.InstanceGroupType
import software.amazon.awssdk.services.emr.model.InstanceState
import software.amazon.awssdk.services.emr.model.StepState

@State(name = "EmrSettings", storages = [Storage("emr.xml")])
class EmrToolWindowSettings : PersistentStateComponent<EmrToolWindowSettings>, IntervalUpdateSettings {
  var customClusterStates: MutableList<EmrClusterState> = EmrClusterState.supportedValues.toMutableList()
  var stepStates: MutableList<StepState> = StepState.knownValues().toMutableList()
  var instanceStates: MutableList<InstanceState> = InstanceState.knownValues().toMutableList()
  var instanceGroupTypes: MutableList<InstanceGroupType> = InstanceGroupType.knownValues().toMutableList()

  private var clustersTableColumns = mutableListOf(
    EmrClusterInfo::name.name,
    EmrClusterInfo::id.name,
    EmrClusterInfo::state.name,
    EmrClusterInfo::created.name,
    EmrClusterInfo::finished.name,
    EmrClusterInfo::normalizedInstanceHours.name,
  )
  val clustersColumnSettings = ColumnVisibilitySettings(clustersTableColumns)

  private var clustersStepsColumns = mutableListOf(
    EmrClusterStepInfo::state.name,
    EmrClusterStepInfo::id.name,
    EmrClusterStepInfo::name.name,
    EmrClusterStepInfo::elapsedTime.name,
    EmrClusterStepInfo::startTime.name,
    EmrClusterStepInfo::endTime.name,
  )
  val clustersStepsColumnsSettings = ColumnVisibilitySettings(clustersStepsColumns)

  private var clustersAppsColumns = mutableListOf(
    EmrClusterAppInfo::name.name,
    EmrClusterAppInfo::version.name,
    EmrClusterAppInfo::url.name,
  )
  val clustersAppsColumnsSettings = ColumnVisibilitySettings(clustersAppsColumns)

  private var clustersInstanceColumns = mutableListOf(
    EmrClusterInstanceInfo::id.name,
    EmrClusterInstanceInfo::type.name,
    EmrClusterInstanceInfo::state.name,
    EmrClusterInstanceInfo::publicUrl.name,
    EmrClusterInstanceInfo::privateUrl.name,
    EmrClusterInstanceInfo::ec2InstanceId.name,
    EmrClusterInstanceInfo::instanceType.name,
    EmrClusterInstanceInfo::market.name
  )
  val clustersInstanceColumnsSettings = ColumnVisibilitySettings(clustersInstanceColumns)

  override var dataUpdateIntervalMillis: Int = 30000
  override var selectedConnectionId: String? = null

  override val configs: MutableMap<String, EmrConfig> = mutableMapOf()

  fun getOrCreateConfig(connectionId: String): EmrConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = EmrConfig()
      configs[connectionId] = config
    }
    return config
  }

  fun saveSelectedCluster(connectionId: String, clusterId: String) {
    getOrCreateConfig(connectionId).selectedCluster = clusterId
  }

  override fun getState(): EmrToolWindowSettings = this

  override fun loadState(state: EmrToolWindowSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    fun getInstance(): EmrToolWindowSettings = service()
  }
}