package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.LinkRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.execution.ui.ConsoleView
import com.jetbrains.bigdatatools.common.monitoring.BrowseOnClick
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.ui.table.renderers.SparkAppStatusIconRenderer
import com.jetbrains.spark.monitoring.ui.table.renderers.SparkByMeIconRenderer
import com.jetbrains.spark.monitoring.util.SparkLocalizedField
import java.util.Date


data class PresentableApplicationInfo(
  val appId: String,
  val name: String,
  val attemptId: String? = "",
  @DateRendering
  val startTime: Date? = null,
  @DateRendering
  val endTime: Date? = null,
  @DateRendering
  val lastUpdated: Date? = null,


  val sparkUser: String = "",


  val appSparkVersion: String = "",

  @field:BrowseOnClick
  @field:LinkRendering
  val logsUrl: String = "",
  @NoRendering
  val console: ConsoleView? = null,

  @NoRendering
  val completed: Boolean = false,


  @field:CustomRendering(SparkAppStatusIconRenderer::class)
  val status: ApplicationStatus = if (!completed)
    ApplicationStatus.RUNNING
  else
    ApplicationStatus.COMPLETE,


  //Requires for app started from IDE
  @NoRendering
  val customId: Int?) : RemoteInfo {

  @NoRendering
  val id: AppAttemptId
    get() = AppAttemptId(appId, attemptId)

  @NoRendering
  val idString: String
    get() = id.toUnderscoreString()

  @DurationRendering
  val duration: Long?
    get() = if (startTime != null && endTime != null)
      endTime.time - startTime.time
    else
      null

  @CustomRendering(SparkByMeIconRenderer::class)
  val byMe: Boolean
    get() = customId != null

  companion object {
    val LIMIT_FILTER = FilterKey("limit")

    val STARTED_BEGIN_FILTER = FilterKey("minDate")
    val STARTED_END_FILTER = FilterKey("maxDate")

    val FINISHED_BEGIN_FILTER = FilterKey("minEndDate")
    val FINISHED_END_FILTER = FilterKey("maxEndDate")

    val renderableColumns: List<SparkLocalizedField<PresentableApplicationInfo>> by lazy {
      listOf(
        SparkLocalizedField(PresentableApplicationInfo::appId, "data.PresentableApplicationInfo.appId"),
        SparkLocalizedField(PresentableApplicationInfo::name, "data.PresentableApplicationInfo.name"),
        SparkLocalizedField(PresentableApplicationInfo::attemptId, "data.PresentableApplicationInfo.attemptId"),
        SparkLocalizedField(PresentableApplicationInfo::startTime, "data.PresentableApplicationInfo.startTime"),
        SparkLocalizedField(PresentableApplicationInfo::endTime, "data.PresentableApplicationInfo.endTime"),
        SparkLocalizedField(PresentableApplicationInfo::lastUpdated, "data.PresentableApplicationInfo.lastUpdated"),
        SparkLocalizedField(PresentableApplicationInfo::sparkUser, "data.PresentableApplicationInfo.sparkUser"),
        SparkLocalizedField(PresentableApplicationInfo::appSparkVersion, "data.PresentableApplicationInfo.appSparkVersion"),
        SparkLocalizedField(PresentableApplicationInfo::logsUrl, "data.PresentableApplicationInfo.logsUrl"),
        SparkLocalizedField(PresentableApplicationInfo::status, "data.PresentableApplicationInfo.status"),
        SparkLocalizedField(PresentableApplicationInfo::duration, "data.PresentableApplicationInfo.duration"),
        SparkLocalizedField(PresentableApplicationInfo::byMe, i18Key = null)
      )
    }

    fun createFor(appInfo: ApplicationInfo, dataManager: SparkDataManager): List<PresentableApplicationInfo> {
      return appInfo.attempts.map { attempt ->
        PresentableApplicationInfo(
          appId = appInfo.id,
          name = appInfo.name,
          attemptId = attempt.attemptId,
          startTime = attempt.startTime?.takeIf { it.time > 0 },
          endTime = attempt.endTime?.takeIf { it.time > 0 },
          lastUpdated = attempt.lastUpdated?.takeIf { it.time > 0 },
          sparkUser = attempt.sparkUser,
          appSparkVersion = attempt.appSparkVersion,
          logsUrl = dataManager.client.getLogsUrl(appInfo.id, attempt.attemptId),
          completed = attempt.completed,
          customId = dataManager.notSyncAppManager.getCustomId(appInfo.id),
          console = dataManager.notSyncAppManager.getConsoleView(dataManager.notSyncAppManager.getCustomId(appInfo.id))
        )
      }

    }
  }
}