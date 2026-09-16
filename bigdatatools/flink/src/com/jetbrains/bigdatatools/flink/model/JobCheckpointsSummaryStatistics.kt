package com.jetbrains.bigdatatools.flink.model

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.flink.util.FlinkLocalizedColumn
import com.jetbrains.bigdatatools.flink.util.NaNTableCellRenderer

data class JobCheckpointsSummaryStatistics(
  val min: Long? = null,
  val max: Long? = null,
  val avg: Long? = null,
  @field:CustomRendering(NaNTableCellRenderer::class)
  val p50: String? = null,
  @field:CustomRendering(NaNTableCellRenderer::class)
  val p90: String? = null,
  @field:CustomRendering(NaNTableCellRenderer::class)
  val p95: String? = null,
  @field:CustomRendering(NaNTableCellRenderer::class)
  val p99: String? = null,
  @field:CustomRendering(NaNTableCellRenderer::class)
  val p999: String? = null
) : RemoteInfo {
  var type: String = ""
  companion object {
    val renderableColumns: List<FlinkLocalizedColumn<JobCheckpointsSummaryStatistics>> by lazy {
      listOf(
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::min, "data.JobCheckpointsSummaryStatistics.min"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::max, "data.JobCheckpointsSummaryStatistics.max"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::avg, "data.JobCheckpointsSummaryStatistics.avg"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::p50, "data.JobCheckpointsSummaryStatistics.p50"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::p90, "data.JobCheckpointsSummaryStatistics.p90"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::p95, "data.JobCheckpointsSummaryStatistics.p95"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::p99, "data.JobCheckpointsSummaryStatistics.p99"),
        FlinkLocalizedColumn(JobCheckpointsSummaryStatistics::p999, "data.JobCheckpointsSummaryStatistics.p999")
      )
    }
  }
}