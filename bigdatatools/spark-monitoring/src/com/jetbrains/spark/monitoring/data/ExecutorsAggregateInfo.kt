package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.monitoring.util.SparkLocalizedField

data class ExecutorsAggregateInfo(
  var name: String,
  var blacklisted: Int,
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
  @field:DataSizeRendering val usedOnHeapStorageMemory: Long,
  @field:DataSizeRendering val usedOffHeapStorageMemory: Long,
  @field:DataSizeRendering val totalOnHeapStorageMemory: Long,
  @field:DataSizeRendering val totalOffHeapStorageMemory: Long) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<ExecutorsAggregateInfo>> by lazy {
      listOf(
        SparkLocalizedField(ExecutorsAggregateInfo::name, "data.ExecutorsAggregateInfo.name"),
        SparkLocalizedField(ExecutorsAggregateInfo::blacklisted, "data.ExecutorsAggregateInfo.blacklisted"),
        SparkLocalizedField(ExecutorsAggregateInfo::rddBlocks, "data.ExecutorsAggregateInfo.rddBlocks"),
        SparkLocalizedField(ExecutorsAggregateInfo::memoryUsed, "data.ExecutorsAggregateInfo.memoryUsed"),
        SparkLocalizedField(ExecutorsAggregateInfo::diskUsed, "data.ExecutorsAggregateInfo.diskUsed"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalCores, "data.ExecutorsAggregateInfo.totalCores"),
        SparkLocalizedField(ExecutorsAggregateInfo::maxTasks, "data.ExecutorsAggregateInfo.maxTasks"),
        SparkLocalizedField(ExecutorsAggregateInfo::activeTasks, "data.ExecutorsAggregateInfo.activeTasks"),
        SparkLocalizedField(ExecutorsAggregateInfo::failedTasks, "data.ExecutorsAggregateInfo.failedTasks"),
        SparkLocalizedField(ExecutorsAggregateInfo::completedTasks, "data.ExecutorsAggregateInfo.completedTasks"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalTasks, "data.ExecutorsAggregateInfo.totalTasks"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalDuration, "data.ExecutorsAggregateInfo.totalDuration"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalGCTime, "data.ExecutorsAggregateInfo.totalGCTime"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalInputBytes, "data.ExecutorsAggregateInfo.totalInputBytes"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalShuffleRead, "data.ExecutorsAggregateInfo.totalShuffleRead"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalShuffleWrite, "data.ExecutorsAggregateInfo.totalShuffleWrite"),
        SparkLocalizedField(ExecutorsAggregateInfo::maxMemory, "data.ExecutorsAggregateInfo.maxMemory"),
        SparkLocalizedField(ExecutorsAggregateInfo::usedOnHeapStorageMemory,
                            "data.ExecutorsAggregateInfo.usedOnHeapStorageMemory"),
        SparkLocalizedField(ExecutorsAggregateInfo::usedOffHeapStorageMemory,
                            "data.ExecutorsAggregateInfo.usedOffHeapStorageMemory"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalOnHeapStorageMemory,
                            "data.ExecutorsAggregateInfo.totalOnHeapStorageMemory"),
        SparkLocalizedField(ExecutorsAggregateInfo::totalOffHeapStorageMemory,
                            "data.ExecutorsAggregateInfo.totalOffHeapStorageMemory"))
    }

    fun createAggregateFrom(executors: List<ExecutorSummary>, isActive: Boolean?): ExecutorsAggregateInfo {
      val filtered = if (isActive != null)
        executors.filter { it.active == isActive }
      else
        executors

      val namePrefix = when (isActive) {
        true -> SMMessagesBundle.message("executors.active")
        false -> SMMessagesBundle.message("executors.dead")
        else -> SMMessagesBundle.message("executors.total")
      }
      return ExecutorsAggregateInfo(
        name = namePrefix + "(${filtered.size})",
        blacklisted = filtered.count { it.blacklisted },
        rddBlocks = filtered.sumOf { it.rddBlocks },
        memoryUsed = filtered.sumOf { it.memoryUsed },
        diskUsed = filtered.sumOf { it.diskUsed },
        totalCores = filtered.sumOf { it.totalCores },
        maxTasks = filtered.sumOf { it.maxTasks },
        activeTasks = filtered.sumOf { it.activeTasks },
        failedTasks = filtered.sumOf { it.failedTasks },
        completedTasks = filtered.sumOf { it.completedTasks },
        totalTasks = filtered.sumOf { it.totalTasks },
        totalDuration = filtered.sumOf { it.totalDuration },
        totalGCTime = filtered.sumOf { it.totalGCTime },
        totalInputBytes = filtered.sumOf { it.totalInputBytes },
        totalShuffleRead = filtered.sumOf { it.totalShuffleRead },
        totalShuffleWrite = filtered.sumOf { it.totalShuffleWrite },
        maxMemory = filtered.sumOf { it.maxMemory },
        usedOnHeapStorageMemory = filtered.sumOf { it.memoryMetrics?.usedOnHeapStorageMemory ?: 0 },
        usedOffHeapStorageMemory = filtered.sumOf { it.memoryMetrics?.usedOffHeapStorageMemory ?: 0 },
        totalOffHeapStorageMemory = filtered.sumOf { it.memoryMetrics?.totalOffHeapStorageMemory ?: 0 },
        totalOnHeapStorageMemory = filtered.sumOf { it.memoryMetrics?.totalOnHeapStorageMemory ?: 0 },
      )
    }
  }
}

