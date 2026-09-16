package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class HiveTableSummaryInfo(
  @NoRendering
  val catalog: String,
  @NoRendering
  val database: String,
  val name: String,
  val location: String,
  val tableType: String,
  val createTime: String,
  val description: String) : RemoteInfo