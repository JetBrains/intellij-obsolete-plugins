package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.squareup.moshi.Json

data class TaskManagerInfo(
  val dataPort: Long? = null,
  val freeResource: TaskManagerTotalResource,
  val freeSlots: Long? = null,
  val hardware: TaskManagerHardware,
  val id: String,
  val jmxPort: Long? = null,
  val memoryConfiguration: TaskManagerMemoryConfiguration,
  val path: String,
  val slotsNumber: Long? = null,
  @Json(name = "timeSinceLastHeartbeat")
  @field:DateRendering(neededAddChecking = true)
  val lastHeartbeat: Long,
  val totalResource: TaskManagerTotalResource
) : RemoteInfo {
  val cpuCores = hardware.cpuCores
  @field:DataSizeRendering
  val physicalMemory = hardware.physicalMemory
  @field:DataSizeRendering
  val freeMemory = hardware.freeMemory
  @field:DataSizeRendering
  val managedMemory = hardware.managedMemory
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<TaskManagerInfo>> by lazy {
      listOf(
        FlinkLocalizedColumn(TaskManagerInfo::dataPort, "data.TaskManagerInfo.dataPort"),
        FlinkLocalizedColumn(TaskManagerInfo::freeResource, "data.TaskManagerInfo.freeResource"),
        FlinkLocalizedColumn(TaskManagerInfo::freeSlots, "data.TaskManagerInfo.freeSlots"),
        FlinkLocalizedColumn(TaskManagerInfo::hardware, "data.TaskManagerInfo.hardware"),
        FlinkLocalizedColumn(TaskManagerInfo::id, "data.TaskManagerInfo.id"),
        FlinkLocalizedColumn(TaskManagerInfo::jmxPort, "data.TaskManagerInfo.jmxPort"),
        FlinkLocalizedColumn(TaskManagerInfo::memoryConfiguration, "data.TaskManagerInfo.memoryConfiguration"),
        FlinkLocalizedColumn(TaskManagerInfo::path, "data.TaskManagerInfo.path"),
        FlinkLocalizedColumn(TaskManagerInfo::slotsNumber, "data.TaskManagerInfo.slotsNumber"),
        FlinkLocalizedColumn(TaskManagerInfo::lastHeartbeat, "data.TaskManagerInfo.lastHeartbeat"),
        FlinkLocalizedColumn(TaskManagerInfo::totalResource, "data.TaskManagerInfo.totalResource"),
        FlinkLocalizedColumn(TaskManagerInfo::cpuCores, "data.TaskManagerInfo.cpuCores"),
        FlinkLocalizedColumn(TaskManagerInfo::physicalMemory, "data.TaskManagerInfo.physicalMemory"),
        FlinkLocalizedColumn(TaskManagerInfo::freeMemory, "data.TaskManagerInfo.freeMemory"),
        FlinkLocalizedColumn(TaskManagerInfo::managedMemory, "data.TaskManagerInfo.managedMemory")
      )
    }
  }
}