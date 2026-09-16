package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DoubleRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.table.renderers.SplitStringRenderer
import com.jetbrains.spark.monitoring.util.SparkLocalizedField

data class Metric(
  @field:CustomRendering(SplitStringRenderer::class)
  val metric: String,
  @field:DoubleRendering val min: Long,
  @field:DoubleRendering val percentile25: Long,
  @field:DoubleRendering val median: Long,
  @field:DoubleRendering val percentile75: Long,
  @field:DoubleRendering val max: Long) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<Metric>> by lazy {
      listOf(
        SparkLocalizedField(Metric::metric, "data.metric.metric"),
        SparkLocalizedField(Metric::min, "data.metric.min"),
        SparkLocalizedField(Metric::percentile25, "data.metric.percentile25"),
        SparkLocalizedField(Metric::median, "data.metric.median"),
        SparkLocalizedField(Metric::percentile75, "data.metric.percentile75"),
        SparkLocalizedField(Metric::max, "data.metric.max")
      )
    }
  }
}
