package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.squareup.moshi.Json

data class JobCheckpointsHistory(
  val alignmentBuffered: Long? = null,
  @Json(name = "checkpoint_type")
  val checkpointType: String,
  @Json(name = "checkpointed_size")
  @field:DataSizeRendering
  val checkpointedDataSize: Long? = null,
  @Json(name = "end_to_end_duration")
  @field:DurationRendering(neededAddChecking = true)
  val endToEndDuration: Long,
  val id: Long? = null,
  @Json(name = "is_savepoint")
  val isSavepoint: Boolean,
  @Json(name = "latest_ack_timestamp")
  @field:DateRendering(neededAddChecking = true)
  val latestAcknowledgement: Long,
  @Json(name = "num_acknowledged_subtasks")
  val acknowledged: Long? = null,
  @Json(name = "num_subtasks")
  val numSubtasks: Long? = null,
  @Json(name = "persisted_data")
  @field:DataSizeRendering
  val persistedData: Long? = null,
  @Json(name = "processed_data")
  @field:DataSizeRendering
  val processedData: Long? = null,
  @Json(name = "state_size")
  val stateSize: Long? = null,
  val status: String,
  @Json(name = "trigger_timestamp")
  @field:DateRendering(neededAddChecking = true)
  val triggerTime: Long
) : RemoteInfo {
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobCheckpointsHistory>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobCheckpointsHistory::alignmentBuffered, "data.JobCheckpointsHistory.alignmentBuffered"),
        FlinkLocalizedColumn(JobCheckpointsHistory::checkpointType, "data.JobCheckpointsHistory.checkpointType"),
        FlinkLocalizedColumn(JobCheckpointsHistory::checkpointedDataSize, "data.JobCheckpointsHistory.checkpointedDataSize"),
        FlinkLocalizedColumn(JobCheckpointsHistory::endToEndDuration, "data.JobCheckpointsHistory.endToEndDuration"),
        FlinkLocalizedColumn(JobCheckpointsHistory::id, "data.JobCheckpointsHistory.id"),
        FlinkLocalizedColumn(JobCheckpointsHistory::isSavepoint, "data.JobCheckpointsHistory.isSavepoint"),
        FlinkLocalizedColumn(JobCheckpointsHistory::latestAcknowledgement, "data.JobCheckpointsHistory.latestAcknowledgement"),
        FlinkLocalizedColumn(JobCheckpointsHistory::acknowledged, "data.JobCheckpointsHistory.acknowledged"),
        FlinkLocalizedColumn(JobCheckpointsHistory::numSubtasks, "data.JobCheckpointsHistory.numSubtasks"),
        FlinkLocalizedColumn(JobCheckpointsHistory::persistedData, "data.JobCheckpointsHistory.persistedData"),
        FlinkLocalizedColumn(JobCheckpointsHistory::processedData, "data.JobCheckpointsHistory.processedData"),
        FlinkLocalizedColumn(JobCheckpointsHistory::stateSize, "data.JobCheckpointsHistory.stateSize"),
        FlinkLocalizedColumn(JobCheckpointsHistory::status, "data.JobCheckpointsHistory.status"),
        FlinkLocalizedColumn(JobCheckpointsHistory::triggerTime, "data.JobCheckpointsHistory.triggerTime")
      )
    }
  }
}