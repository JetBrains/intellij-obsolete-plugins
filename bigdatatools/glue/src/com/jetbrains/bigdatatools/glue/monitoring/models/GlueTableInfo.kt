package com.jetbrains.bigdatatools.glue.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.glue.utils.GlueLocalizedField
import software.amazon.awssdk.services.glue.model.Table

data class GlueTableInfo(@NoRendering
                         val table: Table) : RemoteInfo {
  @NoRendering
  val catalog: String = table.catalogId() ?: ""

  @NoRendering
  val database: String = table.databaseName() ?: ""

  val name: String = table.name() ?: ""
  val description: String = table.description() ?: ""
  val location: String = table.storageDescriptor()?.location() ?: ""
  val tableType: String = table.tableType() ?: ""
  val owner: String = table.owner() ?: ""
  val createdBy: String = table.createdBy() ?: ""
  val createTime: String = table.createTime()?.let { TimeUtils.unixTimeToString(it.toEpochMilli()) } ?: ""
  val lastAccessTime: String = table.lastAccessTime()?.let { TimeUtils.unixTimeToString(it.toEpochMilli()) } ?: ""
  val lastAnalyzedTime: String = table.lastAnalyzedTime()?.let { TimeUtils.unixTimeToString(it.toEpochMilli()) } ?: ""

  companion object {
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<GlueLocalizedField<GlueTableInfo>> by lazy {
      listOf(
        GlueLocalizedField(GlueTableInfo::name, "data.GlueTableInfo.name"),
        GlueLocalizedField(GlueTableInfo::description, "data.GlueTableInfo.description"),
        GlueLocalizedField(GlueTableInfo::location, "data.GlueTableInfo.location"),
        GlueLocalizedField(GlueTableInfo::tableType, "data.GlueTableInfo.tableType"),
        GlueLocalizedField(GlueTableInfo::owner, "data.GlueTableInfo.owner"),
        GlueLocalizedField(GlueTableInfo::createdBy, "data.GlueTableInfo.createdBy"),
        GlueLocalizedField(GlueTableInfo::createTime, "data.GlueTableInfo.createTime"),
        GlueLocalizedField(GlueTableInfo::lastAccessTime, "data.GlueTableInfo.lastAccessTime"),
        GlueLocalizedField(GlueTableInfo::lastAnalyzedTime, "data.GlueTableInfo.lastAnalyzedTime")
      )
    }
  }
}