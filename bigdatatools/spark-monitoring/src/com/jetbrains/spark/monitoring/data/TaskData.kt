package com.jetbrains.spark.monitoring.data

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DurationRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.spark.monitoring.ui.table.renderers.SparkTaskStatusIconRenderer
import com.jetbrains.spark.monitoring.util.SparkLocalizedField
import com.squareup.moshi.Json
import java.util.Date

/*
http://localhost:4040/api/v1/applications/local-1558339557699/stages/0/0/taskList

https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/scheduler/TaskInfo.scala
[ {
  "taskId" : 0,
  "index" : 0,
  "attempt" : 0,
  "launchTime" : "2019-05-20T08:06:06.112GMT",
  "duration" : 278,
  "executorId" : "driver",
  "host" : "localhost",
  "status" : "SUCCESS",
  "taskLocality" : "PROCESS_LOCAL",
  "speculative" : false,
  "accumulatorUpdates" : [ ],
  "taskMetrics" : {
    "executorDeserializeTime" : 19,
    "executorDeserializeCpuTime" : 17618839,
    "executorRunTime" : 111,
    "executorCpuTime" : 71714191,
    "resultSize" : 26826,
    "jvmGcTime" : 0,
    "resultSerializationTime" : 0,
    "memoryBytesSpilled" : 0,
    "diskBytesSpilled" : 0,
    "peakExecutionMemory" : 0,
    "inputMetrics" : {
      "bytesRead" : 0,
      "recordsRead" : 0
    },
    "outputMetrics" : {
      "bytesWritten" : 0,
      "recordsWritten" : 0
    },
    "shuffleReadMetrics" : {
      "remoteBlocksFetched" : 0,
      "localBlocksFetched" : 0,
      "fetchWaitTime" : 0,
      "remoteBytesRead" : 0,
      "remoteBytesReadToDisk" : 0,
      "localBytesRead" : 0,
      "recordsRead" : 0
    },
    "shuffleWriteMetrics" : {
      "bytesWritten" : 0,
      "writeTime" : 0,
      "recordsWritten" : 0
    }
  }
} ]

[ {
  "taskId" : 1585,
  "index" : 0,
  "attempt" : 0,
  "launchTime" : "2019-10-24T13:47:10.735GMT",
  "executorId" : "driver",
  "host" : "localhost",
  "taskLocality" : "ANY",
  "speculative" : false,
  "accumulatorUpdates" : [ ],
  "taskMetrics" : {
    "executorDeserializeTime" : 2,
    "executorDeserializeCpuTime" : 1849219,
    "executorRunTime" : 1,
    "executorCpuTime" : 1461350,
    "resultSize" : 2018,
    "jvmGcTime" : 0,
    "resultSerializationTime" : 0,
    "memoryBytesSpilled" : 0,
    "diskBytesSpilled" : 0,
    "inputMetrics" : {
      "bytesRead" : 0,
      "recordsRead" : 0
    },
    "outputMetrics" : {
      "bytesWritten" : 0,
      "recordsWritten" : 0
    },
    "shuffleReadMetrics" : {
      "remoteBlocksFetched" : 0,
      "localBlocksFetched" : 3,
      "fetchWaitTime" : 0,
      "remoteBytesRead" : 0,
      "localBytesRead" : 177,
      "recordsRead" : 3
    },
    "shuffleWriteMetrics" : {
      "bytesWritten" : 0,
      "writeTime" : 0,
      "recordsWritten" : 0
    }
  }
}

  {
  "taskId" : 1126,
  "index" : 2,
  "attempt" : 0,
  "launchTime" : "2019-10-24T07:37:22.088GMT",
  "executorId" : "driver",
  "host" : "localhost",
  "taskLocality" : "PROCESS_LOCAL",
  "speculative" : false,
  "accumulatorUpdates" : [ ],
  "errorMessage" : "TaskKilled (killed intentionally)"
}

 ]
*/
// @JsonClass(generateAdapter = true)
data class TaskData(
  @field:Json(name = "taskId") @Json(name = "taskId") val id: Int,
  val index: Int,
  val attempt: Int = -1,
  @field:DateRendering val launchTime: Date? = null,
  @field:DateRendering val resultFetchStart: Date? = null,
  val executorId: String = "",
  val host: String,
  @field:Json(name = "taskLocality") @Json(name = "taskLocality") val locality: TaskLocality,
  val speculative: Boolean,
  val accumulatorUpdates: List<AccumulableInfo> = emptyList(),
  @field:DurationRendering var duration: Long = 0,
  @field:CustomRendering(SparkTaskStatusIconRenderer::class) var status: TaskStatus = TaskStatus.UNKNOWN,
  val errorMessage: String? = null,
  @field:NoRendering val taskMetrics: TaskMetrics? = null,
  val executorLogs: Map<String, String> = emptyMap(),
  val schedulerDelay: Long = 0,
  val gettingResultTime: Long = 0) : RemoteInfo {
  companion object {
    val renderableColumns: List<SparkLocalizedField<TaskData>> by lazy {
      listOf(
        SparkLocalizedField(TaskData::id, "data.task.id"),
        SparkLocalizedField(TaskData::index, "data.task.index"),
        SparkLocalizedField(TaskData::attempt, "data.task.attempt"),
        SparkLocalizedField(TaskData::launchTime, "data.task.launchTime"),
        SparkLocalizedField(TaskData::resultFetchStart, "data.task.resultFetchStart"),
        SparkLocalizedField(TaskData::executorId, "data.task.executorId"),
        SparkLocalizedField(TaskData::host, "data.task.host"),
        SparkLocalizedField(TaskData::locality, "data.task.locality"),
        SparkLocalizedField(TaskData::speculative, "data.task.speculative"),
        SparkLocalizedField(TaskData::accumulatorUpdates, "data.task.accumulatorUpdates"),
        SparkLocalizedField(TaskData::duration, "data.task.duration"),
        SparkLocalizedField(TaskData::status, "data.task.status"),
        SparkLocalizedField(TaskData::errorMessage, "data.task.errorMessage"),
        SparkLocalizedField(TaskData::executorLogs, "data.task.executorLogs"),
        SparkLocalizedField(TaskData::schedulerDelay, "data.task.schedulerDelay"),
        SparkLocalizedField(TaskData::gettingResultTime, "data.task.gettingResultTime")
      )
    }
  }
}
