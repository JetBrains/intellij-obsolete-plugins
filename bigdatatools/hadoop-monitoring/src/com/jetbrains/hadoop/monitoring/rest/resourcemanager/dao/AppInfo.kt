package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.FixedProgressRendering
import com.intellij.bigdatatools.coreUi.table.renderers.LinkRendering
import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.BrowseOnClick
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField
import com.jetbrains.hadoop.monitoring.util.NotApplicableRenderer

data class AppInfo(
  // these are ok for any user to see
  val id: String = "",
  val user: String? = null,
  val name: String? = null,
  val queue: String? = null,
  val state: YarnApplicationState? = null,
  val finalStatus: FinalApplicationStatus? = null,

  @field:FixedProgressRendering(100)
  val progress: Float = 0f,

  @field:LinkRendering
  val trackingUrl: String? = null,
  val diagnostics: String? = null,
  val clusterId: Long = 0,
  val applicationType: String? = null,
  val applicationTags: String = "",
  val priority: Int = 0,

  // these are only allowed if acls allow
  @field:UnixtimeRendering
  val startedTime: Long = 0,

  @field:UnixtimeRendering
  val launchTime: Long = 0,

  @field:UnixtimeRendering
  val finishedTime: Long = 0,

  @field:DurationRendering
  val elapsedTime: Long = 0,

  @field:BrowseOnClick
  @field:LinkRendering
  val amContainerLogs: String? = null,

  val amHostHttpAddress: String? = null,

  val amRPCAddress: String? = null,
  val masterNodeId: String? = null,

  @field:CustomRendering(NotApplicableRenderer::class)
  val allocatedMB: Long = 0,

  @field:CustomRendering(NotApplicableRenderer::class)
  val allocatedVCores: Long = 0,

  @field:CustomRendering(NotApplicableRenderer::class)
  val reservedMB: Long = 0,

  @field:CustomRendering(NotApplicableRenderer::class)
  val reservedVCores: Long = 0,

  @field:CustomRendering(NotApplicableRenderer::class)
  val runningContainers: Int = 0,

  @field:DurationRendering
  val memorySeconds: Long = 0,

  @field:DurationRendering
  val vcoreSeconds: Long = 0,
  val queueUsagePercentage: Float = 0f,
  val clusterUsagePercentage: Float = 0f,

  // Strange result from API with duplicate `entry` field
  val resourceSecondsMap: Any? = null,

  // preemption info fields
  val preemptedResourceMB: Long = 0,
  val preemptedResourceVCores: Long = 0,
  val numNonAMContainerPreempted: Int = 0,
  val numAMContainerPreempted: Int = 0,

  @field:DurationRendering
  val preemptedMemorySeconds: Long = 0,

  @field:DurationRendering
  val preemptedVcoreSeconds: Long = 0,
  val preemptedResourceSecondsMap: Any? = null,

  val logAggregationStatus: LogAggregationStatus? = null,
  val unmanagedApplication: Boolean = false,
  val appNodeLabelExpression: String? = null,
  val amNodeLabelExpression: String? = null,
  val resourceInfo: ResourcesInfo? = null,
  val timeouts: AppTimeoutsInfo? = null)
  : RemoteInfo {
  companion object {
    val STATES_FILTER = FilterKey("statesQuery")
    val LIMIT_FILTER = FilterKey("limit")
    val USER_FILTER = FilterKey("userQuery")

    val STARTED_BEGIN_FILTER = FilterKey("startedBegin")
    val STARTED_END_FILTER = FilterKey("startedEnd")

    val FINISHED_BEGIN_FILTER = FilterKey("finishBegin")
    val FINISHED_END_FILTER = FilterKey("finishEnd")

    val renderableColumns: List<HadoopLocalizedField<AppInfo>> by lazy {
      listOf(
        HadoopLocalizedField(AppInfo::id, "data.AppInfo.id"),
        HadoopLocalizedField(AppInfo::user, "data.AppInfo.user"),
        HadoopLocalizedField(AppInfo::name, "data.AppInfo.name"),
        HadoopLocalizedField(AppInfo::queue, "data.AppInfo.queue"),
        HadoopLocalizedField(AppInfo::state, "data.AppInfo.state"),
        HadoopLocalizedField(AppInfo::finalStatus, "data.AppInfo.finalStatus"),
        HadoopLocalizedField(AppInfo::progress, "data.AppInfo.progress"),
        HadoopLocalizedField(AppInfo::trackingUrl, "data.AppInfo.trackingUrl"),
        HadoopLocalizedField(AppInfo::diagnostics, "data.AppInfo.diagnostics"),
        HadoopLocalizedField(AppInfo::clusterId, "data.AppInfo.clusterId"),
        HadoopLocalizedField(AppInfo::applicationType, "data.AppInfo.applicationType"),
        HadoopLocalizedField(AppInfo::applicationTags, "data.AppInfo.applicationTags"),
        HadoopLocalizedField(AppInfo::priority, "data.AppInfo.priority"),
        HadoopLocalizedField(AppInfo::startedTime, "data.AppInfo.startedTime"),
        HadoopLocalizedField(AppInfo::launchTime, "data.AppInfo.launchTime"),
        HadoopLocalizedField(AppInfo::finishedTime, "data.AppInfo.finishedTime"),
        HadoopLocalizedField(AppInfo::elapsedTime, "data.AppInfo.elapsedTime"),
        HadoopLocalizedField(AppInfo::amContainerLogs, "data.AppInfo.amContainerLogs"),
        HadoopLocalizedField(AppInfo::amHostHttpAddress, "data.AppInfo.amHostHttpAddress"),
        HadoopLocalizedField(AppInfo::amRPCAddress, "data.AppInfo.amRPCAddress"),
        HadoopLocalizedField(AppInfo::masterNodeId, "data.AppInfo.masterNodeId"),
        HadoopLocalizedField(AppInfo::allocatedMB, "data.AppInfo.allocatedMB"),
        HadoopLocalizedField(AppInfo::allocatedVCores, "data.AppInfo.allocatedVCores"),
        HadoopLocalizedField(AppInfo::reservedMB, "data.AppInfo.reservedMB"),
        HadoopLocalizedField(AppInfo::reservedVCores, "data.AppInfo.reservedVCores"),
        HadoopLocalizedField(AppInfo::runningContainers, "data.AppInfo.runningContainers"),
        HadoopLocalizedField(AppInfo::memorySeconds, "data.AppInfo.memorySeconds"),
        HadoopLocalizedField(AppInfo::vcoreSeconds, "data.AppInfo.vcoreSeconds"),
        HadoopLocalizedField(AppInfo::queueUsagePercentage, "data.AppInfo.queueUsagePercentage"),
        HadoopLocalizedField(AppInfo::clusterUsagePercentage, "data.AppInfo.clusterUsagePercentage"),
        HadoopLocalizedField(AppInfo::resourceSecondsMap, "data.AppInfo.resourceSecondsMap"),
        HadoopLocalizedField(AppInfo::preemptedResourceMB, "data.AppInfo.preemptedResourceMB"),
        HadoopLocalizedField(AppInfo::preemptedResourceVCores, "data.AppInfo.preemptedResourceVCores"),
        HadoopLocalizedField(AppInfo::numNonAMContainerPreempted, "data.AppInfo.numNonAMContainerPreempted"),
        HadoopLocalizedField(AppInfo::numAMContainerPreempted, "data.AppInfo.numAMContainerPreempted"),
        HadoopLocalizedField(AppInfo::preemptedMemorySeconds, "data.AppInfo.preemptedMemorySeconds"),
        HadoopLocalizedField(AppInfo::preemptedVcoreSeconds, "data.AppInfo.preemptedVcoreSeconds"),
        HadoopLocalizedField(AppInfo::preemptedResourceSecondsMap, "data.AppInfo.preemptedResourceSecondsMap"),
        HadoopLocalizedField(AppInfo::logAggregationStatus, "data.AppInfo.logAggregationStatus"),
        HadoopLocalizedField(AppInfo::unmanagedApplication, "data.AppInfo.unmanagedApplication"),
        HadoopLocalizedField(AppInfo::appNodeLabelExpression, "data.AppInfo.appNodeLabelExpression"),
        HadoopLocalizedField(AppInfo::amNodeLabelExpression, "data.AppInfo.amNodeLabelExpression"),
        HadoopLocalizedField(AppInfo::resourceInfo, "data.AppInfo.resourceInfo"),
        HadoopLocalizedField(AppInfo::timeouts, "data.AppInfo.timeouts")
      )
    }
  }
}