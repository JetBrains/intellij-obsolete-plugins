package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveLocalizedField

data class HiveSchemaInfo(
  val name: String,
  val dataType: String,
  val comment: String) : RemoteInfo {
  companion object {
    val renderableColumns: List<HiveLocalizedField<HiveSchemaInfo>> by lazy {
      listOf(
        HiveLocalizedField(HiveSchemaInfo::name, "data.HiveSchemaInfo.name"),
        HiveLocalizedField(HiveSchemaInfo::dataType, "data.HiveSchemaInfo.dataType"),
        HiveLocalizedField(HiveSchemaInfo::comment, "data.HiveSchemaInfo.comment")
      )
    }
  }
}