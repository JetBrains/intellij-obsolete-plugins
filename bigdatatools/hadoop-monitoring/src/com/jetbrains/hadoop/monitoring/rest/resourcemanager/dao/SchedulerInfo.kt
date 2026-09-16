package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

@Suppress("unused")
open class SchedulerInfo : RemoteInfo {
  var schedulerName: String? = null
  var minAllocResource: ResourceInfo? = null
  var maxAllocResource: ResourceInfo? = null
  var schedulingResourceTypes: Set<SchedulerResourceTypes>? = null
  var maximumClusterPriority = 0

  enum class Type {
    fairScheduler, fifoScheduler, capacityScheduler
  }

  companion object {
    val renderableColumns: List<HadoopLocalizedField<SchedulerInfo>> by lazy {
      listOf(
        HadoopLocalizedField(SchedulerInfo::schedulerName, "data.SchedulerInfo.schedulerName"),
        HadoopLocalizedField(SchedulerInfo::minAllocResource, "data.SchedulerInfo.minAllocResource"),
        HadoopLocalizedField(SchedulerInfo::maxAllocResource, "data.SchedulerInfo.maxAllocResource"),
        HadoopLocalizedField(SchedulerInfo::schedulingResourceTypes, "data.SchedulerInfo.schedulingResourceTypes"),
        HadoopLocalizedField(SchedulerInfo::maximumClusterPriority, "data.SchedulerInfo.maximumClusterPriority")
      )
    }
  }
}