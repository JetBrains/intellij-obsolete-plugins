package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.LinkArrayRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.table.renderers.RightAlignedRenderer
import com.jetbrains.spark.monitoring.ui.table.renderers.SqlStatusIconRenderer
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.monitoring.util.SparkLocalizedField
import org.jetbrains.annotations.Nls

enum class SqlInfoStatus(@Nls override val text: String) : DisplayableStatus {
  COMPLETED(SMMessagesBundle.message("status.completed")),
  RUNNING(SMMessagesBundle.message("status.running")),
  FAILED(SMMessagesBundle.message("status.failed"))
}

data class SqlInfo(
  @field:CustomRendering(SqlStatusIconRenderer::class) val status: SqlInfoStatus,
  val id: Int,
  @field:NoRendering
  val descriptionLink: String,
  val descriptionShort: String,
  @field:NoRendering
  val descriptionFull: String,
  val submitted: String,

  @field:CustomRendering(RightAlignedRenderer::class)
  val duration: String,

  @field:LinkArrayRendering
  val succeededJobs: List<String> = emptyList(),

  @field:LinkArrayRendering
  val runningJobs: List<String> = emptyList(),

  @field:LinkArrayRendering
  val failedJobs: List<String> = emptyList()
) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<SqlInfo>> by lazy {
      listOf(
        SparkLocalizedField(SqlInfo::status, "data.SqlInfo.status"),
        SparkLocalizedField(SqlInfo::id, "data.SqlInfo.id"),
        SparkLocalizedField(SqlInfo::descriptionShort, "data.SqlInfo.descriptionShort"),
        SparkLocalizedField(SqlInfo::submitted, "data.SqlInfo.submitted"),
        SparkLocalizedField(SqlInfo::duration, "data.SqlInfo.duration"),
        SparkLocalizedField(SqlInfo::succeededJobs, "data.SqlInfo.succeededJobs"),
        SparkLocalizedField(SqlInfo::runningJobs, "data.SqlInfo.runningJobs"),
        SparkLocalizedField(SqlInfo::failedJobs, "data.SqlInfo.failedJobs")
      )
    }
  }
}