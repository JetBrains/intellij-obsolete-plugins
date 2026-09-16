package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.util.SparkLocalizedField

// @JsonClass(generateAdapter = true)
data class RDDStorageInfo(
  val id: Int,
  val name: String,
  val numPartitions: Int,
  val numCachedPartitions: Int,
  val storageLevel: String,
  @field:DataSizeRendering val memoryUsed: Long,
  @field:DataSizeRendering val diskUsed: Long,

  val dataDistribution: List<RDDDataDistribution>,
  val partitions: List<RDDPartitionInfo>
) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<RDDStorageInfo>> by lazy {
      listOf(
        SparkLocalizedField(RDDStorageInfo::id, "data.RDDStorageInfo.id"),
        SparkLocalizedField(RDDStorageInfo::name, "data.RDDStorageInfo.name"),
        SparkLocalizedField(RDDStorageInfo::numPartitions, "data.RDDStorageInfo.numPartitions"),
        SparkLocalizedField(RDDStorageInfo::numCachedPartitions, "data.RDDStorageInfo.numCachedPartitions"),
        SparkLocalizedField(RDDStorageInfo::storageLevel, "data.RDDStorageInfo.storageLevel"),
        SparkLocalizedField(RDDStorageInfo::memoryUsed, "data.RDDStorageInfo.memoryUsed"),
        SparkLocalizedField(RDDStorageInfo::diskUsed, "data.RDDStorageInfo.diskUsed")
      )
    }
  }
}
