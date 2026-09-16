package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class AppAttemptInfo(
  val id: Int = 0,
  @field:UnixtimeRendering
  val startTime: Long = 0,
  @field:UnixtimeRendering
  val finishedTime: Long = 0,
  val containerId: String? = null,
  val nodeHttpAddress: String? = null,
  val nodeId: String? = null,
  val logsLink: String? = null,
  val blacklistedNodes: String? = null,
  val nodesBlacklistedBySystem: String? = null,
  val appAttemptId: String? = null
) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<AppAttemptInfo>> by lazy {
      listOf(
        HadoopLocalizedField(AppAttemptInfo::id, "data.AppAttemptInfo.id"),
        HadoopLocalizedField(AppAttemptInfo::startTime, "data.AppAttemptInfo.startTime"),
        HadoopLocalizedField(AppAttemptInfo::finishedTime, "data.AppAttemptInfo.finishedTime"),
        HadoopLocalizedField(AppAttemptInfo::containerId, "data.AppAttemptInfo.containerId"),
        HadoopLocalizedField(AppAttemptInfo::nodeHttpAddress, "data.AppAttemptInfo.nodeHttpAddress"),
        HadoopLocalizedField(AppAttemptInfo::nodeId, "data.AppAttemptInfo.nodeId"),
        HadoopLocalizedField(AppAttemptInfo::logsLink, "data.AppAttemptInfo.logsLink"),
        HadoopLocalizedField(AppAttemptInfo::blacklistedNodes, "data.AppAttemptInfo.blacklistedNodes"),
        HadoopLocalizedField(AppAttemptInfo::nodesBlacklistedBySystem, "data.AppAttemptInfo.nodesBlacklistedBySystem"),
        HadoopLocalizedField(AppAttemptInfo::appAttemptId, "data.AppAttemptInfo.appAttemptId")
      )
    }
  }
}