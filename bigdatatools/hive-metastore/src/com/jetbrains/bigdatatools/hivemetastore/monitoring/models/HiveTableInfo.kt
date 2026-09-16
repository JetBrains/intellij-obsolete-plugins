package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveLocalizedField
import org.apache.hadoop.hive.metastore.api.Table

data class HiveTableInfo(
  @NoRendering
  val origin: Table,
  @NoRendering
  val catalog: String,
  @NoRendering
  val database: String,
  val name: String,
  val location: String,
  val tableType: String,
  val createTime: String,
  val description: String) : RemoteInfo {

  companion object {
    val TYPE_FILTER = FilterKey("types")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<HiveLocalizedField<HiveTableInfo>> by lazy {
      listOf(
        HiveLocalizedField(HiveTableInfo::name, "data.HiveTableInfo.name"),
        HiveLocalizedField(HiveTableInfo::location, "data.HiveTableInfo.location"),
        HiveLocalizedField(HiveTableInfo::tableType, "data.HiveTableInfo.tableType"),
        HiveLocalizedField(HiveTableInfo::createTime, "data.HiveTableInfo.createTime"),
        HiveLocalizedField(HiveTableInfo::description, "data.HiveTableInfo.description")
      )
    }
  }
}