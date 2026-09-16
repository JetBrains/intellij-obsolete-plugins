package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.squareup.moshi.Json

data class JarInfo(
  @field:NoRendering
  val id: String,
  val name: String,
  @field:DateRendering(neededAddChecking = true)
  @Json(name = "uploaded")
  val uploadedTime: Long,
  @field:NoRendering
  val entry: List<EntryClassInfo> = emptyList()
) : RemoteInfo {
  val entryClass: String = entry.joinToString(separator = " ") { it.name }

  companion object {
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<FlinkLocalizedColumn<JarInfo>> by lazy {
      listOf(
        FlinkLocalizedColumn(JarInfo::name, "data.JarInfo.name"),
        FlinkLocalizedColumn(JarInfo::uploadedTime, "data.JarInfo.uploadedTime"),
        FlinkLocalizedColumn(JarInfo::entryClass, "data.JarInfo.entryClass")
      )
    }
  }
}