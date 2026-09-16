package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.bigdatatools.emr.util.EmrLocalizedColumn
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import software.amazon.awssdk.services.emr.model.HadoopJarStepConfig
import software.amazon.awssdk.services.emr.model.KeyValue
import software.amazon.awssdk.services.emr.model.StepConfig
import software.amazon.awssdk.services.emr.model.StepState
import software.amazon.awssdk.services.emr.model.StepSummary
import java.util.Date

data class EmrClusterStepInfo(val name: String = "",
                              val id: String = "",
                              val state: StepState = StepState.UNKNOWN_TO_SDK_VERSION,
                              @field:DateRendering
                              val startTime: Date? = null,
                              @field:DateRendering
                              val endTime: Date? = null,
                              @field:NoRendering
                              val originalObject: StepSummary,
                              val config: StepConfig) : RemoteInfo {
  @field:DurationRendering
  val elapsedTime: Long? = if (startTime != null && endTime != null)
    endTime.time - startTime.time
  else
    null

  companion object {
    val STATES_FILTER = FilterKey("states")
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<EmrLocalizedColumn<EmrClusterStepInfo>> by lazy {
      listOf(
        EmrLocalizedColumn(EmrClusterStepInfo::name, "data.emr.cluster.step.name"),
        EmrLocalizedColumn(EmrClusterStepInfo::id, "data.emr.cluster.step.id"),
        EmrLocalizedColumn(EmrClusterStepInfo::state, "data.emr.cluster.step.state"),
        EmrLocalizedColumn(EmrClusterStepInfo::startTime, "data.emr.cluster.step.startTime"),
        EmrLocalizedColumn(EmrClusterStepInfo::endTime, "data.emr.cluster.step.endTime"),
        EmrLocalizedColumn(EmrClusterStepInfo::config, "data.emr.cluster.step.config"),
        EmrLocalizedColumn(EmrClusterStepInfo::elapsedTime, "data.emr.cluster.step.elapsedTime")
      )
    }

    fun getFrom(info: StepSummary): EmrClusterStepInfo {
      val originConfig = info.config()
      val jarStepConfig = HadoopJarStepConfig.builder()
        .jar(originConfig.jar() ?: "")
        .args(originConfig.args())
        .mainClass(originConfig.mainClass() ?: "")
        .properties(originConfig.properties().map { KeyValue.builder().key(it.key).value(it.value).build() })
        .build()
      val stepConfig = StepConfig
        .builder()
        .name(info.name() ?: "").actionOnFailure(info.actionOnFailure()).hadoopJarStep(jarStepConfig).build()
      return EmrClusterStepInfo(name = info.name() ?: "",
                                id = info.id() ?: "",
                                state = StepState.knownValues().firstOrNull { it == info.status().state() } ?: StepState.FAILED,
                                startTime = info.status()?.timeline()?.startDateTime()?.let { Date.from(it) },
                                endTime = info.status()?.timeline()?.endDateTime()?.let { Date.from(it) },
                                config = stepConfig,
                                originalObject = info)
    }
  }
}

val StepState.isRunning: Boolean
  get() = this in setOf(StepState.RUNNING, StepState.PENDING)