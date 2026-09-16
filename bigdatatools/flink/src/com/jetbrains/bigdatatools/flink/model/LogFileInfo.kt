package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn

data class LogFileInfo(
  val mtime: Long? = null,
  val name: String,
  @field:DataSizeRendering
  val size: Long
) : RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<LogFileInfo>> by lazy {
      listOf(
        FlinkLocalizedColumn(LogFileInfo::mtime, "data.LogFileInfo.mtime"),
        FlinkLocalizedColumn(LogFileInfo::name, "data.LogFileInfo.name"),
        FlinkLocalizedColumn(LogFileInfo::size, "data.LogFileInfo.size")
      )
    }
  }
}