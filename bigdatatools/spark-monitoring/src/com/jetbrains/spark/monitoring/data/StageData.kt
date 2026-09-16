package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DataSizeRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.ProgressRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.table.renderers.RightAlignedRenderer
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.spark.monitoring.ui.table.renderers.SparkStageStatusIconRenderer
import com.jetbrains.spark.monitoring.util.SparkLocalizedField
import com.squareup.moshi.Json
import java.util.Date

/*

[ {
  "status" : "COMPLETE",
  "stageId" : 3,
  "attemptId" : 0,
  "numTasks" : 1,
  "numActiveTasks" : 0,
  "numCompleteTasks" : 1,
  "numFailedTasks" : 0,
  "numKilledTasks" : 0,
  "numCompletedIndices" : 1,
  "executorRunTime" : 20,
  "executorCpuTime" : 19163961,
  "submissionTime" : "2019-05-20T08:06:08.887GMT",
  "firstTaskLaunchedTime" : "2019-05-20T08:06:08.889GMT",
  "completionTime" : "2019-05-20T08:06:08.912GMT",
  "inputBytes" : 0,
  "inputRecords" : 0,
  "outputBytes" : 0,
  "outputRecords" : 0,
  "shuffleReadBytes" : 11374,
  "shuffleReadRecords" : 200,
  "shuffleWriteBytes" : 0,
  "shuffleWriteRecords" : 0,
  "memoryBytesSpilled" : 0,
  "diskBytesSpilled" : 0,
  "name" : "count at <console>:36",
  "description" : "Started by: anonymous",
  "details" : "org.apache.spark.sql.Dataset.count(Dataset.scala:2830)\n$line196878499930.$read$$iw$$iw$$iw$$iw$$iw$$iw$$iw$$iw$$iw$$iw.<init>(<console>:36)\n$line196878499930.$read$$iw$$iw$$iw$$iw$$iw$$iw$$iw$$iw$$iw.<init>(<console>:42)\n$line196878499930.$read$$iw$$iw$$iw$$iw$$iw$$iw$$iw$$iw.<init>(<console>:44)\n$line196878499930.$read$$iw$$iw$$iw$$iw$$iw$$iw$$iw.<init>(<console>:46)\n$line196878499930.$read$$iw$$iw$$iw$$iw$$iw$$iw.<init>(<console>:48)\n$line196878499930.$read$$iw$$iw$$iw$$iw$$iw.<init>(<console>:50)\n$line196878499930.$read$$iw$$iw$$iw$$iw.<init>(<console>:52)\n$line196878499930.$read$$iw$$iw$$iw.<init>(<console>:54)\n$line196878499930.$read$$iw$$iw.<init>(<console>:56)\n$line196878499930.$read$$iw.<init>(<console>:58)\n$line196878499930.$read.<init>(<console>:60)\n$line196878499930.$read$.<init>(<console>:64)\n$line196878499930.$read$.<clinit>(<console>)\n$line196878499930.$eval$.$print$lzycompute(<console>:7)\n$line196878499930.$eval$.$print(<console>:6)\n$line196878499930.$eval.$print(<console>)\nsun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\nsun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\nsun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)",
  "schedulingPool" : "default",
  "rddIds" : [ 19, 17, 18 ],
  "accumulatorUpdates" : [ ],
  "killedTasksSummary" : { }
}]
 */
// @JsonClass(generateAdapter = true)
data class StageData(

  @field:Json(name = "stageId") @Json(name = "stageId") val id: Int,

  val name: String,

  @field:CustomRendering(SparkStageStatusIconRenderer::class)
  var status: StageStatus,

  @field:ProgressRendering(maxPropertyName = "numTasks")
  var numCompleteTasks: Int,

  var numActiveTasks: Int = 0,
  var numFailedTasks: Int = 0,
  var numKilledTasks: Int = 0,
  var numTasks: Int = numActiveTasks + numFailedTasks + numKilledTasks + numCompleteTasks,

  val attemptId: Int = 0,

  var numCompletedIndices: Int = 0,
  val executorDeserializeTime: Long = 0,
  val executorDeserializeCpuTime: Long = 0,
  var executorRunTime: Long = 0,
  var executorCpuTime: Long = 0,
  var resultSize: Long = 0,
  var jvmGcTime: Long = 0,
  val resultSerializationTime: Long = 0,
  val peakExecutionMemory: Long = 0,
  val shuffleRemoteBlocksFetched: Long = 0,
  val shuffleLocalBlocksFetched: Long = 0,
  val shuffleFetchWaitTime: Long = 0,
  val shuffleRemoteBytesRead: Long = 0,
  val shuffleRemoteBytesReadToDisk: Long = 0,
  val shuffleLocalBytesRead: Long = 0,
  val shuffleWriteTime: Long = 0,


  @field:DateRendering
  var submissionTime: Date? = null,

  @field:DateRendering
  var firstTaskLaunchedTime: Date? = null,

  @field:DateRendering
  var completionTime: Date? = null,

  @field:DataSizeRendering
  var inputBytes: Long,

  var inputRecords: Long,
  var failureReason: String = "",

  @field:DataSizeRendering
  var outputBytes: Long,

  var outputRecords: Long,

  @field:DataSizeRendering
  var shuffleReadBytes: Long,

  var shuffleReadRecords: Long,

  @field:DataSizeRendering
  var shuffleWriteBytes: Long,

  var shuffleWriteRecords: Long,

  @field:DataSizeRendering
  var memoryBytesSpilled: Long,

  @field:DataSizeRendering
  var diskBytesSpilled: Long,

  var description: String = "",
  var details: String = "",
  var schedulingPool: String = "",
  var rddIds: List<Int> = emptyList(),
  var accumulatorUpdates: List<AccumulableInfo> = emptyList(),
  var tasks: Map<Long, TaskData> = emptyMap(),
  var executorSummary: Map<String, ExecutorStageSummary> = emptyMap(),
  var killedTasksSummary: Map<String, Int> = emptyMap(),
  val resourceProfileId: Int = 0,

  @field:CustomRendering(RightAlignedRenderer::class)
  val duration: String = TimeUtils.intervalAsString(firstTaskLaunchedTime, completionTime)
) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<StageData>> by lazy {
      listOf(
        SparkLocalizedField(StageData::id, "data.stage.id"),
        SparkLocalizedField(StageData::name, "data.stage.name"),
        SparkLocalizedField(StageData::status, "data.stage.status"),
        SparkLocalizedField(StageData::numCompleteTasks, "data.stage.numCompleteTasks"),
        SparkLocalizedField(StageData::numActiveTasks, "data.stage.numActiveTasks"),
        SparkLocalizedField(StageData::numFailedTasks, "data.stage.numFailedTasks"),
        SparkLocalizedField(StageData::numKilledTasks, "data.stage.numKilledTasks"),
        SparkLocalizedField(StageData::numTasks, "data.stage.numTasks"),
        SparkLocalizedField(StageData::attemptId, "data.stage.attemptId"),
        SparkLocalizedField(StageData::numCompletedIndices, "data.stage.numCompletedIndices"),
        SparkLocalizedField(StageData::executorDeserializeTime, "data.stage.executorDeserializeTime"),
        SparkLocalizedField(StageData::executorDeserializeCpuTime, "data.stage.executorDeserializeCpuTime"),
        SparkLocalizedField(StageData::executorRunTime, "data.stage.executorRunTime"),
        SparkLocalizedField(StageData::executorCpuTime, "data.stage.executorCpuTime"),
        SparkLocalizedField(StageData::resultSize, "data.stage.resultSize"),
        SparkLocalizedField(StageData::jvmGcTime, "data.stage.jvmGcTime"),
        SparkLocalizedField(StageData::resultSerializationTime, "data.stage.resultSerializationTime"),
        SparkLocalizedField(StageData::peakExecutionMemory, "data.stage.peakExecutionMemory"),
        SparkLocalizedField(StageData::shuffleRemoteBlocksFetched, "data.stage.shuffleRemoteBlocksFetched"),
        SparkLocalizedField(StageData::shuffleLocalBlocksFetched, "data.stage.shuffleLocalBlocksFetched"),
        SparkLocalizedField(StageData::shuffleFetchWaitTime, "data.stage.shuffleFetchWaitTime"),
        SparkLocalizedField(StageData::shuffleRemoteBytesRead, "data.stage.shuffleRemoteBytesRead"),
        SparkLocalizedField(StageData::shuffleRemoteBytesReadToDisk, "data.stage.shuffleRemoteBytesReadToDisk"),
        SparkLocalizedField(StageData::shuffleLocalBytesRead, "data.stage.shuffleLocalBytesRead"),
        SparkLocalizedField(StageData::shuffleWriteTime, "data.stage.shuffleWriteTime"),
        SparkLocalizedField(StageData::submissionTime, "data.stage.submissionTime"),
        SparkLocalizedField(StageData::firstTaskLaunchedTime, "data.stage.firstTaskLaunchedTime"),
        SparkLocalizedField(StageData::completionTime, "data.stage.completionTime"),
        SparkLocalizedField(StageData::inputBytes, "data.stage.inputBytes"),
        SparkLocalizedField(StageData::inputRecords, "data.stage.inputRecords"),
        SparkLocalizedField(StageData::failureReason, "data.stage.failureReason"),
        SparkLocalizedField(StageData::outputBytes, "data.stage.outputBytes"),
        SparkLocalizedField(StageData::outputRecords, "data.stage.outputRecords"),
        SparkLocalizedField(StageData::shuffleReadBytes, "data.stage.shuffleReadBytes"),
        SparkLocalizedField(StageData::shuffleReadRecords, "data.stage.shuffleReadRecords"),
        SparkLocalizedField(StageData::shuffleWriteBytes, "data.stage.shuffleWriteBytes"),
        SparkLocalizedField(StageData::shuffleWriteRecords, "data.stage.shuffleWriteRecords"),
        SparkLocalizedField(StageData::memoryBytesSpilled, "data.stage.memoryBytesSpilled"),
        SparkLocalizedField(StageData::diskBytesSpilled, "data.stage.diskBytesSpilled"),
        SparkLocalizedField(StageData::description, "data.stage.description"),
        SparkLocalizedField(StageData::details, "data.stage.details"),
        SparkLocalizedField(StageData::schedulingPool, "data.stage.schedulingPool"),
        SparkLocalizedField(StageData::rddIds, "data.stage.rddIds"),
        SparkLocalizedField(StageData::accumulatorUpdates, "data.stage.accumulatorUpdates"),
        SparkLocalizedField(StageData::tasks, "data.stage.tasks"),
        SparkLocalizedField(StageData::executorSummary, "data.stage.executorSummary"),
        SparkLocalizedField(StageData::killedTasksSummary, "data.stage.killedTasksSummary"),
        SparkLocalizedField(StageData::resourceProfileId, "data.stage.resourceProfileId"),
        SparkLocalizedField(StageData::duration, "data.stage.duration")
      )
    }
  }
}
