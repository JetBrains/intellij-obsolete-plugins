package com.jetbrains.bigdatatools.dataproc.model

import com.google.cloud.dataproc.v1.InstanceGroupConfig
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.dataproc.util.DataprocLocalizedField

data class DataprocVmInstanceInfo(val name: String,
                                  val role: VmInstanceRole,
                                  @NoRendering val clusterInfo: DataprocClusterInfo) : RemoteInfo {
  companion object {
    fun createFrom(config: InstanceGroupConfig, role: VmInstanceRole, cluster: DataprocClusterInfo): List<DataprocVmInstanceInfo> {
      val names = config.instanceNamesList?.toList() ?: emptyList()
      return names.map { DataprocVmInstanceInfo(it, role, cluster) }
    }

    val renderableColumns: List<DataprocLocalizedField<DataprocVmInstanceInfo>> by lazy {
      listOf(
        DataprocLocalizedField(DataprocVmInstanceInfo::name, "data.web.interfaceInfo.name"),
        DataprocLocalizedField(DataprocVmInstanceInfo::role, "data.web.interfaceInfo.role")
      )
    }
  }
}

