package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveLocalizedField
import org.apache.hadoop.hive.metastore.Warehouse
import org.apache.hadoop.hive.metastore.api.Database

data class HiveDatabaseInfo(@NoRendering val database: Database) : RemoteInfo {
  val name: String = database.name ?: ""
  val description: String = database.description ?: ""
  val catalog: String = database.catalogName ?: Warehouse.DEFAULT_CATALOG_NAME

  companion object {
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<HiveLocalizedField<HiveDatabaseInfo>> by lazy {
      listOf(
        HiveLocalizedField(HiveDatabaseInfo::name, "data.HiveDatabaseInfo.name"),
        HiveLocalizedField(HiveDatabaseInfo::description, "data.HiveDatabaseInfo.description"),
        HiveLocalizedField(HiveDatabaseInfo::catalog, "data.HiveDatabaseInfo.catalog")
      )
    }
  }
}