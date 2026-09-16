package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.util.SparkLocalizedField

data class RDDPartitionInfo(
  val blockName: String,
  val storageLevel: String,
  @field:DataSizeRendering val memoryUsed: Long,
  @field:DataSizeRendering val diskUsed: Long,
  val executors: List<String>
) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<RDDPartitionInfo>> by lazy {
      listOf(
        SparkLocalizedField(RDDPartitionInfo::blockName, "data.RDDPartitionInfo.blockName"),
        SparkLocalizedField(RDDPartitionInfo::storageLevel, "data.RDDPartitionInfo.storageLevel"),
        SparkLocalizedField(RDDPartitionInfo::memoryUsed, "data.RDDPartitionInfo.memoryUsed"),
        SparkLocalizedField(RDDPartitionInfo::diskUsed, "data.RDDPartitionInfo.diskUsed"),
        SparkLocalizedField(RDDPartitionInfo::executors, "data.RDDPartitionInfo.executors")
      )
    }
  }
}