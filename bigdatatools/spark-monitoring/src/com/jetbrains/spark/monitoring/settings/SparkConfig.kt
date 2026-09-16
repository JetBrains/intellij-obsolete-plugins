package com.jetbrains.spark.monitoring.settings

import com.jetbrains.bigdatatools.common.ui.filter.DatePeriodType
import com.jetbrains.spark.monitoring.data.ApplicationStatus
import com.jetbrains.spark.monitoring.data.JobExecutionStatus
import com.jetbrains.spark.monitoring.data.SqlInfoStatus
import com.jetbrains.spark.monitoring.data.StageStatus
import java.util.Date

// Config for specific connection id
data class SparkConfig(
  /** Filters for states. */
  var showOnlyMyTasks: Boolean = false,
  var jobStatuses: HashSet<JobExecutionStatus> = HashSet<JobExecutionStatus>().apply { addAll(JobExecutionStatus.entries) },
  var applicationStatuses: MutableSet<ApplicationStatus> = ApplicationStatus.entries.toMutableSet(),
  var stageStatuses: MutableSet<StageStatus> = HashSet<StageStatus>().apply { addAll(StageStatus.entries) },
  var sqlStatuses: MutableSet<SqlInfoStatus> = HashSet<SqlInfoStatus>().apply { addAll(SqlInfoStatus.entries) },


  var appFilter: String = "",

  var selectedJobId: String? = null,
  var selectedStageId: String? = null,

  var applicationsLimit: Int? = 100,

  var applicationStartedPeriodType: DatePeriodType = DatePeriodType.SPECIFIED,
  var applicationsStartedBegin: Date? = null,
  var applicationsStartedEnd: Date? = null,

  var applicationFinishedPeriodType: DatePeriodType = DatePeriodType.SPECIFIED,
  var applicationsFinishedBegin: Date? = null,
  var applicationsFinishedEnd: Date? = null,

  var jobsSplitterProportion: Float = 0.35f,
  var stagesSplitterProportion: Float = 0.6f,
  var sqlSplitterProportion: Float = 0.6f,
  var storageSplitterProportion: Float = 0.4f,
  var storageDetailsSplitterProportion: Float = 0.5f,

  var showDetails: Boolean = false,
  var showTasks: Boolean = true,

  var showStoragePartitions: Boolean = true,
  var showStorageDistribution: Boolean = true
)