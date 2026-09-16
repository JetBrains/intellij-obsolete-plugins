package com.jetbrains.bigdatatools.dataproc.model

import com.google.cloud.dataproc.v1.Job
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.dataproc.util.DataprocLocalizedField
import java.util.Date

data class DataprocJobInfo(@NoRendering val job: Job) : RemoteInfo {
  val id: String = job.reference.jobId
  val status = job.status.state.name
  val type = job.typeJobCase.name
  val cluster = job.placement.clusterName
  val startTime = job.statusHistoryList?.first()?.stateStartTime?.seconds?.let {
    Date(it * 1000)
  }

  val elapsedTime = startTime?.time?.let {
    (job.status.stateStartTime?.seconds?.times(1000))?.minus(it)?.let {
      TimeUtils.intervalAsString(it)
    }
  }
  val labels = job.labelsMap.entries.joinToString(separator = ",") { it.key + ":" + it.value }

  companion object {
    val STATES_FILTER = FilterKey("states")
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<DataprocLocalizedField<DataprocJobInfo>> by lazy {
      listOf(
        DataprocLocalizedField(DataprocJobInfo::id, "data.jobInfo.id"),
        DataprocLocalizedField(DataprocJobInfo::status, "data.jobInfo.status"),
        DataprocLocalizedField(DataprocJobInfo::type, "data.jobInfo.type"),
        DataprocLocalizedField(DataprocJobInfo::cluster, "data.jobInfo.cluster"),
        DataprocLocalizedField(DataprocJobInfo::startTime, "data.jobInfo.startTime"),
        DataprocLocalizedField(DataprocJobInfo::elapsedTime, "data.jobInfo.elapsedTime"),
        DataprocLocalizedField(DataprocJobInfo::labels, "data.jobInfo.labels")
      )
    }

    val Job.jobId: String
      get() = reference.jobId
  }
}