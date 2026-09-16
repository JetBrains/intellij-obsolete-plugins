package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.monitoring.util.SparkLocalizedField
import com.squareup.moshi.Json
import java.util.Date

// @JsonClass(generateAdapter = true)
data class ExecutorSummary(
  val id: String,
  @field:Json(name = "hostPort") @Json(name = "hostPort") val address: String,
  @field:Json(name = "isActive") @Json(name = "isActive") var active: Boolean,
  @field:Json(name = "isBlacklisted") @Json(name = "isBlacklisted") var blacklisted: Boolean = false,
  val rddBlocks: Long,
  @field:DataSizeRendering val memoryUsed: Long,
  @field:DataSizeRendering val diskUsed: Long,
  val totalCores: Int,
  val maxTasks: Int,
  val activeTasks: Int,
  val failedTasks: Int,
  val completedTasks: Int,
  val totalTasks: Int,

  @field:DurationRendering val totalDuration: Long,
  @field:DurationRendering val totalGCTime: Long,

  @field:DataSizeRendering val totalInputBytes: Long,
  @field:DataSizeRendering val totalShuffleRead: Long,
  @field:DataSizeRendering val totalShuffleWrite: Long,
  @field:DataSizeRendering val maxMemory: Long,
  val addTime: Date? = null,
  val removeTime: Date? = null,
  val removeReason: String? = null,
  @NoRendering
  val executorLogs: Map<String, String> = mapOf(),
  val logs: String = SMMessagesBundle.message("column.show.logs"),
  val memoryMetrics: MemoryMetrics? = null,
  val blacklistedInStages: List<Int> = emptyList(),
  val attributes: Map<String, String> = mapOf(),
  val resources: Map<String, ResourceInformation> = mapOf(),
  val resourceProfileId: Int = 0
) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<ExecutorSummary>> by lazy {
      listOf(
        SparkLocalizedField(ExecutorSummary::id, "data.ExecutorSummary.id"),
        SparkLocalizedField(ExecutorSummary::address, "data.ExecutorSummary.address"),
        SparkLocalizedField(ExecutorSummary::active, "data.ExecutorSummary.active"),
        SparkLocalizedField(ExecutorSummary::blacklisted, "data.ExecutorSummary.blacklisted"),
        SparkLocalizedField(ExecutorSummary::rddBlocks, "data.ExecutorSummary.rddBlocks"),
        SparkLocalizedField(ExecutorSummary::memoryUsed, "data.ExecutorSummary.memoryUsed"),
        SparkLocalizedField(ExecutorSummary::diskUsed, "data.ExecutorSummary.diskUsed"),
        SparkLocalizedField(ExecutorSummary::totalCores, "data.ExecutorSummary.totalCores"),
        SparkLocalizedField(ExecutorSummary::maxTasks, "data.ExecutorSummary.maxTasks"),
        SparkLocalizedField(ExecutorSummary::activeTasks, "data.ExecutorSummary.activeTasks"),
        SparkLocalizedField(ExecutorSummary::failedTasks, "data.ExecutorSummary.failedTasks"),
        SparkLocalizedField(ExecutorSummary::completedTasks, "data.ExecutorSummary.completedTasks"),
        SparkLocalizedField(ExecutorSummary::totalTasks, "data.ExecutorSummary.totalTasks"),
        SparkLocalizedField(ExecutorSummary::totalDuration, "data.ExecutorSummary.totalDuration"),
        SparkLocalizedField(ExecutorSummary::totalGCTime, "data.ExecutorSummary.totalGCTime"),
        SparkLocalizedField(ExecutorSummary::totalInputBytes, "data.ExecutorSummary.totalInputBytes"),
        SparkLocalizedField(ExecutorSummary::totalShuffleRead, "data.ExecutorSummary.totalShuffleRead"),
        SparkLocalizedField(ExecutorSummary::totalShuffleWrite, "data.ExecutorSummary.totalShuffleWrite"),
        SparkLocalizedField(ExecutorSummary::maxMemory, "data.ExecutorSummary.maxMemory"),
        SparkLocalizedField(ExecutorSummary::addTime, "data.ExecutorSummary.addTime"),
        SparkLocalizedField(ExecutorSummary::removeTime, "data.ExecutorSummary.removeTime"),
        SparkLocalizedField(ExecutorSummary::removeReason, "data.ExecutorSummary.removeReason"),
        SparkLocalizedField(ExecutorSummary::logs, "column.show.logs"),
        SparkLocalizedField(ExecutorSummary::memoryMetrics, "data.ExecutorSummary.memoryMetrics"),
        SparkLocalizedField(ExecutorSummary::blacklistedInStages, "data.ExecutorSummary.blacklistedInStages"),
        SparkLocalizedField(ExecutorSummary::attributes, "data.ExecutorSummary.attributes"),
        SparkLocalizedField(ExecutorSummary::resources, "data.ExecutorSummary.resources"),
        SparkLocalizedField(ExecutorSummary::resourceProfileId, "data.ExecutorSummary.resourceProfileId")
      )
    }
  }
}

