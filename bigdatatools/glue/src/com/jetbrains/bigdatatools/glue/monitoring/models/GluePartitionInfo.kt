package com.jetbrains.bigdatatools.glue.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.glue.utils.GlueLocalizedField
import software.amazon.awssdk.services.glue.model.Column
import software.amazon.awssdk.services.glue.model.Partition

data class GluePartitionInfo(@NoRendering
                             val partition: Partition, @NoRendering val partitionKeys: List<Column>) : RemoteInfo {
  @NoRendering
  val partitionValues = partition.values()?.toList() ?: listOf<String>()

  val name: String = partitionValues.zip(partitionKeys).joinToString { (it.second.name() ?: "") + "=" + it.first }
  val location: String = partition.storageDescriptor()?.location() ?: ""

  companion object {
    val LIMIT_FILTER = FilterKey("limit")

    val renderableColumns: List<GlueLocalizedField<GluePartitionInfo>> by lazy {
      listOf(
        GlueLocalizedField(GluePartitionInfo::name, "data.GluePartitionInfo.name"),
        GlueLocalizedField(GluePartitionInfo::location, "data.GluePartitionInfo.location")
      )
    }
  }
}