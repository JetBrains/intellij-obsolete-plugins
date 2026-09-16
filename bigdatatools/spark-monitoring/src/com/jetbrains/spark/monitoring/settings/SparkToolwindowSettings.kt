package com.jetbrains.spark.monitoring.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.delegate.Delegate
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.spark.monitoring.data.ExecutorSummary
import com.jetbrains.spark.monitoring.data.ExecutorsAggregateInfo
import com.jetbrains.spark.monitoring.data.JobData
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.TableType

/**
 * User: Dmitry.Naydanov
 * Date: 2019-04-01.
 */
@Suppress("MemberVisibilityCanBePrivate")
@State(name = "SparkMonitoringSettings", storages = [Storage("SparkMonitoringSettings.xml")])
class SparkToolwindowSettings : PersistentStateComponent<SparkToolwindowSettings>, IntervalUpdateSettings {
  var version = 1

  var defaultApplicationsColumns = mutableListOf(
    PresentableApplicationInfo::byMe.name,
    PresentableApplicationInfo::appId.name,
    PresentableApplicationInfo::name.name,
    PresentableApplicationInfo::status.name,
    PresentableApplicationInfo::sparkUser.name,
    PresentableApplicationInfo::duration.name,
    PresentableApplicationInfo::startTime.name,
    PresentableApplicationInfo::endTime.name,
    PresentableApplicationInfo::logsUrl.name)

  var defaultJobsColumns = mutableListOf(JobData::id.name,
                                         JobData::status.name,
                                         JobData::name.name,
                                         JobData::submissionTime.name,
                                         JobData::numCompletedTasks.name,
                                         JobData::visualization.name)

  var stagesFullColumns = mutableListOf("id", "name", "duration", "numCompleteTasks", "status", "inputBytes", "outputBytes",
                                        "shuffleReadBytes", "shuffleWriteBytes")
  var stagesShortColumns = mutableListOf("id", "name", "duration", "numCompleteTasks", "status", "inputBytes", "outputBytes",
                                         "shuffleReadBytes", "shuffleWriteBytes")

  var defaultTasksColumns = mutableListOf("id", "index", "launchTime", "host", "speculative", "duration", "status")

  var defaultExecutorsColumns = mutableListOf(
    ExecutorSummary::id.name,
    ExecutorSummary::logs.name,
    ExecutorSummary::address.name,
    ExecutorSummary::active.name,
    ExecutorSummary::activeTasks.name,
    ExecutorSummary::failedTasks.name,
    ExecutorSummary::completedTasks.name,
    ExecutorSummary::totalTasks.name)

  var defaultExecutorsAggregateColumns = mutableListOf(
    ExecutorsAggregateInfo::name.name,
    ExecutorsAggregateInfo::rddBlocks.name,
    ExecutorsAggregateInfo::memoryUsed.name,
    ExecutorsAggregateInfo::maxMemory.name,
    ExecutorsAggregateInfo::diskUsed.name,
    ExecutorsAggregateInfo::totalCores.name,
    ExecutorsAggregateInfo::activeTasks.name,
    ExecutorsAggregateInfo::failedTasks.name,
    ExecutorsAggregateInfo::completedTasks.name,
    ExecutorsAggregateInfo::totalDuration.name,
    ExecutorsAggregateInfo::totalInputBytes.name,
    ExecutorsAggregateInfo::totalShuffleRead.name,
    ExecutorsAggregateInfo::totalShuffleWrite.name,
    ExecutorsAggregateInfo::blacklisted.name)

  var defaultStoragesColumns = mutableListOf("id", "name", "numPartitions", "numCachedPartitions", "storageLevel", "memoryUsed", "diskUsed")

  var defaultStoragesDistributionColumns = mutableListOf("address", "onHeapMemoryUsed", "onHeapMemoryRemaining", "offHeapMemoryUsed",
                                                         "offHeapMemoryRemaining", "diskUsed")

  var defaultStoragesPartitionColumns = mutableListOf("blockName", "storageLevel", "memoryUsed", "diskUsed", "executors")

  var defaultSqlColumns = mutableListOf("id", "status", "descriptionShort", "submitted", "duration", "succeededJobs", "runningJobs",
                                        "failedJobs")

  override var selectedConnectionId: String? = ""

  @Transient
  val delegateTimeUpdate = Delegate<Int, Unit>()

  override var dataUpdateIntervalMillis = 30000
    set(value) {
      if (value == field)
        return

      field = value
      delegateTimeUpdate.notify(value)
    }

  override var configs: MutableMap<String, SparkConfig> = mutableMapOf()

  /** Filters for different tables columns. */
  val applicationsColumnSettings = ColumnVisibilitySettings(defaultApplicationsColumns)
  val jobsColumnSettings = ColumnVisibilitySettings(defaultJobsColumns)
  val stagesFullColumnSettings = ColumnVisibilitySettings(stagesFullColumns)
  val stagesShortColumnSettings = ColumnVisibilitySettings(stagesShortColumns)
  val tasksColumnSettings = ColumnVisibilitySettings(defaultTasksColumns)
  val executorsColumnSettings = ColumnVisibilitySettings(defaultExecutorsColumns)
  val executorsAggregateColumnSettings = ColumnVisibilitySettings(defaultExecutorsAggregateColumns)
  val storagesColumnSettings = ColumnVisibilitySettings(defaultStoragesColumns)
  val storagesDistributionColumnSettings = ColumnVisibilitySettings(defaultStoragesDistributionColumns)
  val storagesPartitionColumnSettings = ColumnVisibilitySettings(defaultStoragesPartitionColumns)
  val sqlColumnSettings = ColumnVisibilitySettings(defaultSqlColumns)


  init {
    applicationsColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Application, value)
    }
    jobsColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Job, value)
    }
    stagesFullColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Stage, value)
    }
    tasksColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Task, value)
    }
    executorsColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Executor, value)
    }
    storagesColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Storage, value)
    }
    sqlColumnSettings.onColumnVisibilityChanged += { _, value ->
      SparkMonitoringUsagesCollector.columnVisibilityChangedEvent.log(TableType.Sql, value)
    }
  }

  fun refreshAllApplications() {
    DriverManager.getDriversForAllOpenProjects().filterIsInstance<SparkMonitoringDriver>().forEach {
      it.dataManager.updater.invokeRefreshModel(it.dataManager.applications)
    }
  }

  // region PersistentStateComponent
  override fun getState(): SparkToolwindowSettings = this

  override fun loadState(state: SparkToolwindowSettings) {
    XmlSerializerUtil.copyBean(state, this)

    if (version == 1) {
      defaultApplicationsColumns = mutableListOf("appId", "name", "status", "attemptId", "sparkUser", "duration", "startTime", "endTime")
      version = 2
    }

    applicationsColumnSettings.visibleColumns = defaultApplicationsColumns
    jobsColumnSettings.visibleColumns = defaultJobsColumns
    stagesFullColumnSettings.visibleColumns = stagesFullColumns
    stagesShortColumnSettings.visibleColumns = stagesShortColumns
    tasksColumnSettings.visibleColumns = defaultTasksColumns
    executorsColumnSettings.visibleColumns = defaultExecutorsColumns
    storagesColumnSettings.visibleColumns = defaultStoragesColumns
    sqlColumnSettings.visibleColumns = defaultSqlColumns
  }
  // endregion PersistentStateComponent

  fun getSparkConfigOrDefault(connectionId: String): SparkConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = SparkConfig()
      configs[connectionId] = config
    }
    return config
  }

  fun getJobsSplitterProportion(connectionId: String): Float {
    return configs[connectionId]?.jobsSplitterProportion ?: 0.35f
  }

  fun setJobsSplitterProportion(connectionId: String, proportion: Float) {
    getSparkConfigOrDefault(connectionId).jobsSplitterProportion = proportion
  }

  fun getStagesSplitterProportion(connectionId: String): Float {
    return configs[connectionId]?.stagesSplitterProportion ?: 0.6f
  }

  fun setStagesSplitterProportion(connectionId: String, proportion: Float) {
    getSparkConfigOrDefault(connectionId).stagesSplitterProportion = proportion
  }

  fun getSqlSplitterProportion(connectionId: String): Float {
    return configs[connectionId]?.sqlSplitterProportion ?: 0.6f
  }

  fun setSqlSplitterProportion(connectionId: String, proportion: Float) {
    getSparkConfigOrDefault(connectionId).sqlSplitterProportion = proportion
  }

  fun setStorageSplitterProportion(connectionId: String, proportion: Float) {
    getSparkConfigOrDefault(connectionId).storageSplitterProportion = proportion
  }

  fun getStorageSplitterProportion(connectionId: String): Float {
    return configs[connectionId]?.storageSplitterProportion ?: 0.5f
  }

  fun setStorageDetailsSplitterProportion(connectionId: String, proportion: Float) {
    getSparkConfigOrDefault(connectionId).storageDetailsSplitterProportion = proportion
  }

  fun getStorageDetailsSplitterProportion(connectionId: String): Float {
    return configs[connectionId]?.storageDetailsSplitterProportion ?: 0.5f
  }

  fun isDetailsShown(connectionId: String): Boolean {
    return configs[connectionId]?.showDetails ?: false
  }

  fun setDetailsShown(connectionId: String, shown: Boolean) {
    if (!shown && configs[connectionId] == null) {
      return
    }
    getSparkConfigOrDefault(connectionId).showDetails = shown
  }

  fun isTasksShown(connectionId: String): Boolean {
    return configs[connectionId]?.showTasks ?: true
  }

  fun setTasksShown(connectionId: String, shown: Boolean) {
    if (shown && configs[connectionId] == null) {
      return
    }
    getSparkConfigOrDefault(connectionId).showTasks = shown
  }

  fun getSelectedJobId(connectionId: String): String? {
    return configs[connectionId]?.selectedJobId
  }

  fun setSelectedJobId(connectionId: String, selectedJobId: String?) {
    if (selectedJobId == null && configs[connectionId] == null) {
      return
    }
    getSparkConfigOrDefault(connectionId).selectedJobId = selectedJobId
  }

  fun setSelectedStageId(connectionId: String, selectedStageId: String?) {
    if (selectedStageId == null && configs[connectionId] == null) {
      return
    }
    getSparkConfigOrDefault(connectionId).selectedStageId = selectedStageId
  }

  companion object {
    fun getInstance(): SparkToolwindowSettings = service()
  }
}