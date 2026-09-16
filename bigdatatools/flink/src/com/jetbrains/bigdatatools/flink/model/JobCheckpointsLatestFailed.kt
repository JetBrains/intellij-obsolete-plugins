package com.jetbrains.bigdatatools.flink.model

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.squareup.moshi.Json

data class JobCheckpointsLatestFailed(
  @Json(name = "alignment_buffered")
  val alignmentBuffered: Long? = null,
  @Json(name = "checkpoint_type")
  val checkpointType: String,
  @Json(name = "checkpointed_size")
  val checkpointedSize: Long? = null,
  @Json(name = "end_to_end_duration")
  val endToEndDuration: Long? = null,
  val failure_message: String,
  val failure_timestamp: Long? = null,
  val id: Long? = null,
  @Json(name = "is_savepoint")
  val isSavepoint: Boolean,
  @Json(name = "latest_ack_timestamp")
  val latestAckTimestamp: Long? = null,
  @Json(name = "num_acknowledged_subtasks")
  val numAcknowledgedSubtasks: Long? = null,
  @Json(name = "num_subtasks")
  val numSubtasks: Long? = null,
  @Json(name = "persisted_data")
  val persistedData: Long? = null,
  @Json(name = "processed_data")
  val processedData: Long? = null,
  @Json(name = "state_size")
  val stateSize: Long? = null,
  val trigger_timestamp: Long? = null
) : RemoteInfo