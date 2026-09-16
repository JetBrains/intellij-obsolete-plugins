package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.util.SparkLocalizedField

data class RDDDataDistribution(
  val address: String,
  @field:DataSizeRendering val memoryUsed: Long,
  @field:DataSizeRendering val memoryRemaining: Long,
  @field:DataSizeRendering val diskUsed: Long,
  @field:DataSizeRendering val onHeapMemoryUsed: Long? = null,
  @field:DataSizeRendering val offHeapMemoryUsed: Long? = null,
  @field:DataSizeRendering val onHeapMemoryRemaining: Long? = null,
  @field:DataSizeRendering val offHeapMemoryRemaining: Long? = null
) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<RDDDataDistribution>> by lazy {
      listOf(
        SparkLocalizedField(RDDDataDistribution::address, "data.RDDDataDistribution.address"),
        SparkLocalizedField(RDDDataDistribution::memoryUsed, "data.RDDDataDistribution.memoryUsed"),
        SparkLocalizedField(RDDDataDistribution::memoryRemaining, "data.RDDDataDistribution.memoryRemaining"),
        SparkLocalizedField(RDDDataDistribution::diskUsed, "data.RDDDataDistribution.diskUsed"),
        SparkLocalizedField(RDDDataDistribution::onHeapMemoryUsed, "data.RDDDataDistribution.onHeapMemoryUsed"),
        SparkLocalizedField(RDDDataDistribution::offHeapMemoryUsed, "data.RDDDataDistribution.offHeapMemoryUsed"),
        SparkLocalizedField(RDDDataDistribution::onHeapMemoryRemaining, "data.RDDDataDistribution.onHeapMemoryRemaining"),
        SparkLocalizedField(RDDDataDistribution::offHeapMemoryRemaining, "data.RDDDataDistribution.offHeapMemoryRemaining")
      )
    }
  }
}