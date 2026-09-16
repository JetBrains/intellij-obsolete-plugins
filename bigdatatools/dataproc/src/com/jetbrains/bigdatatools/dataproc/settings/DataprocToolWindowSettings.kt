package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterState
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobState
import com.jetbrains.bigdatatools.dataproc.model.DataprocVmInstanceInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo

@State(name = "DataprocSettings", storages = [Storage("bdt-dataproc.xml")])
class DataprocToolWindowSettings : PersistentStateComponent<DataprocToolWindowSettings>, IntervalUpdateSettings {
  var lastTimeUpdateKey: Long = 0

  var customClusterStates: MutableList<DataprocClusterState> = DataprocClusterState.entries.toMutableList()
  var customJobStates: MutableList<DataprocJobState> = DataprocJobState.entries.toMutableList()

  private var clustersTableColumns = mutableListOf(
    DataprocClusterInfo::name.name,
    DataprocClusterInfo::state.name,
    DataprocClusterInfo::zone.name,
    DataprocClusterInfo::totalWorkers.name,
    DataprocClusterInfo::scheduledDeletion.name,
    DataprocClusterInfo::created.name
  )
  val clustersColumnSettings = ColumnVisibilitySettings(clustersTableColumns)

  private var clustersStepsColumns = mutableListOf(
    DataprocJobInfo::id.name,
    DataprocJobInfo::status.name,
    DataprocJobInfo::type.name,
    DataprocJobInfo::startTime.name,
    DataprocJobInfo::elapsedTime.name,
    DataprocJobInfo::labels.name,
  )
  val jobsColumnsSettings = ColumnVisibilitySettings(clustersStepsColumns)

  val vmInstanceColumnsSettings = ColumnVisibilitySettings(mutableListOf(
    DataprocVmInstanceInfo::name.name,
    DataprocVmInstanceInfo::role.name))

  val webIntefracesColumnsSettings = ColumnVisibilitySettings(mutableListOf(
    DataprocWebInterfaceInfo::name.name,
    DataprocWebInterfaceInfo::url.name))

  override var dataUpdateIntervalMillis: Int = 30000
  override var selectedConnectionId: String? = null

  override val configs: MutableMap<String, DataprocConfig> = mutableMapOf()

  fun getOrCreateConfig(connectionId: String): DataprocConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = DataprocConfig()
      configs[connectionId] = config
    }
    return config
  }

  fun saveSelectedCluster(connectionId: String, clusterId: String) {
    getOrCreateConfig(connectionId).selectedCluster = clusterId
  }

  override fun getState(): DataprocToolWindowSettings = this

  override fun loadState(state: DataprocToolWindowSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    fun getInstance(): DataprocToolWindowSettings = service()
  }
}