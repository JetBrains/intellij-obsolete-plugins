package com.jetbrains.bigdatatools.flink.toolwindow.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.flink.model.JarInfo
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsHistory
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsSummaryStatistics
import com.jetbrains.bigdatatools.flink.model.JobExceptionEntry
import com.jetbrains.bigdatatools.flink.model.JobExceptionType
import com.jetbrains.bigdatatools.flink.model.JobExecutionStatus
import com.jetbrains.bigdatatools.flink.model.JobInfo
import com.jetbrains.bigdatatools.flink.model.JobManagerConfig
import com.jetbrains.bigdatatools.flink.model.JobVertex
import com.jetbrains.bigdatatools.flink.model.LogFileInfo
import com.jetbrains.bigdatatools.flink.model.TaskManagerInfo

@State(name = "FlinkSettings", storages = [Storage("flink.xml")])
class FlinkToolWindowSettings : PersistentStateComponent<FlinkToolWindowSettings>, IntervalUpdateSettings {
  override var selectedConnectionId: String? = null

  var jobStatus: MutableList<JobExecutionStatus> = JobExecutionStatus.entries.toMutableList()

  private val jarsTableColumns = mutableListOf(JarInfo::name.name,
                                               JarInfo::uploadedTime.name,
                                               JarInfo::entryClass.name)
  val jarsColumnSettings = ColumnVisibilitySettings(jarsTableColumns)

  private val jobsTableColumns = mutableListOf(JobInfo::jobName.name,
                                               JobInfo::status.name,
                                               JobInfo::startTime.name,
                                               JobInfo::duration.name,
                                               JobInfo::endTime.name,
                                               JobInfo::tasks.name,
                                               JobInfo::visualization.name)
  val jobsColumnSettings = ColumnVisibilitySettings(jobsTableColumns)

  private val jobOverviewColumns = mutableListOf(JobVertex::name.name,
                                                 JobVertex::status.name,
                                                 JobVertex::bytesReceived.name,
                                                 JobVertex::recordsReceived.name,
                                                 JobVertex::bytesSent.name,
                                                 JobVertex::recordsSent.name,
                                                 JobVertex::parallelism.name,
                                                 JobVertex::startTime.name,
                                                 JobVertex::duration.name,
                                                 JobVertex::endTime.name)
  val jobOverviewSettings = ColumnVisibilitySettings(jobOverviewColumns)

  private val jobAllExceptionsColumns = mutableListOf(JobExceptionType::exception.name,
                                                      JobExceptionType::timestamp.name,
                                                      JobExceptionType::task.name,
                                                      JobExceptionType::location.name)
  val jobAllExceptionsSettings = ColumnVisibilitySettings(jobAllExceptionsColumns)

  private val jobExceptionHistoryColumns = mutableListOf(JobExceptionEntry::timestamp.name,
                                                         JobExceptionEntry::exceptionName.name,
                                                         JobExceptionEntry::taskName.name,
                                                         JobExceptionEntry::location.name,
                                                         JobExceptionEntry::stacktrace.name,
                                                         JobExceptionEntry::concurrentExceptions.name)
  val jobExceptionHistorySettings = ColumnVisibilitySettings(jobExceptionHistoryColumns)

  private val jobCheckpointHistoryColumns = mutableListOf(JobCheckpointsHistory::id.name,
                                                          JobCheckpointsHistory::status.name,
                                                          JobCheckpointsHistory::acknowledged.name,
                                                          JobCheckpointsHistory::triggerTime.name,
                                                          JobCheckpointsHistory::latestAcknowledgement.name,
                                                          JobCheckpointsHistory::endToEndDuration.name,
                                                          JobCheckpointsHistory::checkpointedDataSize.name,
                                                          JobCheckpointsHistory::processedData.name,
                                                          JobCheckpointsHistory::persistedData.name)
  val jobCheckpointHistorySettings = ColumnVisibilitySettings(jobCheckpointHistoryColumns)

  private val jobCheckpointSummaryColumns = mutableListOf(JobCheckpointsSummaryStatistics::type.name,
                                                          JobCheckpointsSummaryStatistics::min.name,
                                                          JobCheckpointsSummaryStatistics::max.name,
                                                          JobCheckpointsSummaryStatistics::avg.name,
                                                          JobCheckpointsSummaryStatistics::p50.name,
                                                          JobCheckpointsSummaryStatistics::p90.name,
                                                          JobCheckpointsSummaryStatistics::p95.name,
                                                          JobCheckpointsSummaryStatistics::p99.name,
                                                          JobCheckpointsSummaryStatistics::p999.name)
  val jobCheckpointSummarySettings = ColumnVisibilitySettings(jobCheckpointSummaryColumns)

  private val taskManagerColumns = mutableListOf(TaskManagerInfo::id.name,
                                                 TaskManagerInfo::path.name,
                                                 TaskManagerInfo::dataPort.name,
                                                 TaskManagerInfo::lastHeartbeat.name,
                                                 TaskManagerInfo::slotsNumber.name,
                                                 TaskManagerInfo::freeSlots.name,
                                                 TaskManagerInfo::cpuCores.name,
                                                 TaskManagerInfo::physicalMemory.name,
                                                 TaskManagerInfo::freeMemory.name,
                                                 TaskManagerInfo::managedMemory.name)
  val taskManagerSettings = ColumnVisibilitySettings(taskManagerColumns)

  private val jobManagerConfigColumns = mutableListOf(JobManagerConfig::key.name,
                                                      JobManagerConfig::value.name)
  val jobManagerConfigSettings = ColumnVisibilitySettings(jobManagerConfigColumns)

  private val logFileInfoColumns = mutableListOf(LogFileInfo::name.name,
                                                 LogFileInfo::size.name)
  val logFileInfoSettings = ColumnVisibilitySettings(logFileInfoColumns)

  override val configs: MutableMap<String, FlinkConfig> = mutableMapOf()

  fun getOrCreateConfig(connectionId: String): FlinkConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = FlinkConfig()
      configs[connectionId] = config
    }
    return config
  }

  override var dataUpdateIntervalMillis: Int = 30000

  override fun getState(): FlinkToolWindowSettings = this

  override fun loadState(state: FlinkToolWindowSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  fun saveSelectedJob(connectionId: String, jobId: String) {
    getOrCreateConfig(connectionId).selectedJob = jobId
  }

  companion object {
    fun getInstance(): FlinkToolWindowSettings = service()
  }
}