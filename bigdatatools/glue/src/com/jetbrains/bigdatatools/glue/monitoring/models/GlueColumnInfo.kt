package com.jetbrains.bigdatatools.glue.monitoring.models

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.glue.utils.GlueLocalizedField
import software.amazon.awssdk.services.glue.model.Column

data class GlueColumnInfo(val column: Column) : RemoteInfo {
  val name: String = column.name() ?: ""
  val dataType: String = column.type() ?: ""
  val comment: String = column.comment() ?: ""

  companion object {
    val renderableColumns: List<GlueLocalizedField<GlueColumnInfo>> by lazy {
      listOf(
        GlueLocalizedField(GlueColumnInfo::column, "data.GlueColumnInfo.column"),
        GlueLocalizedField(GlueColumnInfo::name, "data.GlueColumnInfo.name"),
        GlueLocalizedField(GlueColumnInfo::dataType, "data.GlueColumnInfo.dataType"),
        GlueLocalizedField(GlueColumnInfo::comment, "data.GlueColumnInfo.comment")
      )
    }
  }
}