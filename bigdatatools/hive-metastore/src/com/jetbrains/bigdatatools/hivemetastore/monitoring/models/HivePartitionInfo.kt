package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveLocalizedField
import org.apache.hadoop.hive.metastore.api.Partition

data class HivePartitionInfo(
  val name: String,
  val location: String,
  @NoRendering
  val origin: Partition) : RemoteInfo {
  companion object {
    val LIMIT_FILTER = FilterKey("limit")

    val renderableColumns: List<HiveLocalizedField<HivePartitionInfo>> by lazy {
      listOf(
        HiveLocalizedField(HivePartitionInfo::name, "data.HivePartitionInfo.name"),
        HiveLocalizedField(HivePartitionInfo::location, "data.HivePartitionInfo.location")
      )
    }
  }
}