package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.jetbrains.bigdatatools.flink.util.SnakeCaseRenderer
import com.squareup.moshi.Json

data class JobCheckpointsConfig(
  @field:DurationRendering(neededAddChecking = true)
  @Json(name = "aligned_checkpoint_timeout")
  val alignedCheckpointTimeout: Long,
  @Json(name = "checkpoint_storage")
  val checkpointStorage: String,
  @Json(name = "checkpoints_after_tasks_finish")
  val checkpointsAfterTasksFinish: Boolean?,
  @field:DurationRendering(neededAddChecking = true)
  val interval: Long,
  val externalization: JobCheckpointsConfigExternalization,
  @Json(name = "max_concurrent")
  val maxConcurrent: Long? = null,
  @field:DurationRendering(neededAddChecking = true)
  @Json(name = "min_pause")
  val minPause: Long,
  @field:CustomRendering(SnakeCaseRenderer::class)
  val mode: String = "",
  @Json(name = "state_backend")
  val stateBackend: String,
  @field:DurationRendering(neededAddChecking = true)
  val timeout: Long,
  @Json(name = "tolerable_failed_checkpoints")
  val tolerableFailedCheckpoints: Long? = null,
  @Json(name = "unaligned_checkpoints")
  val unalignedCheckpoints: Boolean
) : RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobCheckpointsConfig>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobCheckpointsConfig::alignedCheckpointTimeout, "data.JobCheckpointsConfig.alignedCheckpointTimeout"),
        FlinkLocalizedColumn(JobCheckpointsConfig::checkpointStorage, "data.JobCheckpointsConfig.checkpointStorage"),
        FlinkLocalizedColumn(JobCheckpointsConfig::checkpointsAfterTasksFinish, "data.JobCheckpointsConfig.checkpointsAfterTasksFinish"),
        FlinkLocalizedColumn(JobCheckpointsConfig::interval, "data.JobCheckpointsConfig.interval"),
        FlinkLocalizedColumn(JobCheckpointsConfig::externalization, "data.JobCheckpointsConfig.externalization"),
        FlinkLocalizedColumn(JobCheckpointsConfig::maxConcurrent, "data.JobCheckpointsConfig.maxConcurrent"),
        FlinkLocalizedColumn(JobCheckpointsConfig::minPause, "data.JobCheckpointsConfig.minPause"),
        FlinkLocalizedColumn(JobCheckpointsConfig::mode, "data.JobCheckpointsConfig.mode"),
        FlinkLocalizedColumn(JobCheckpointsConfig::stateBackend, "data.JobCheckpointsConfig.stateBackend"),
        FlinkLocalizedColumn(JobCheckpointsConfig::timeout, "data.JobCheckpointsConfig.timeout"),
        FlinkLocalizedColumn(JobCheckpointsConfig::tolerableFailedCheckpoints, "data.JobCheckpointsConfig.tolerableFailedCheckpoints"),
        FlinkLocalizedColumn(JobCheckpointsConfig::unalignedCheckpoints, "data.JobCheckpointsConfig.unalignedCheckpoints")
      )
    }
  }
}