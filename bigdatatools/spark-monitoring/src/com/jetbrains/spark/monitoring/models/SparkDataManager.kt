package com.jetbrains.spark.monitoring.models

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.applyIf
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.spark.monitoring.connection.SparkMonitoringRestClient
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.ApplicationEnvironmentInfo
import com.jetbrains.spark.monitoring.data.ApplicationInfo
import com.jetbrains.spark.monitoring.data.ApplicationStatus
import com.jetbrains.spark.monitoring.data.ExecutorSummary
import com.jetbrains.spark.monitoring.data.ExecutorsAggregateInfo
import com.jetbrains.spark.monitoring.data.JobData
import com.jetbrains.spark.monitoring.data.Metric
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.data.RDDStorageInfo
import com.jetbrains.spark.monitoring.data.SqlInfo
import com.jetbrains.spark.monitoring.data.StageData
import com.jetbrains.spark.monitoring.data.TaskData
import com.jetbrains.spark.monitoring.data.TaskMetrics
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.text.SimpleDateFormat
import java.util.Date


class SparkDataManager(override val connectionData: SparkConnectionData,
                       project: Project?) : MonitoringDataManager(project, SparkToolwindowSettings.getInstance()) {
  override val client = SparkMonitoringRestClient(project, connectionData)

  val notSyncAppManager = SparkNotSyncAppManager(this).also { Disposer.register(this, it) }

  val applicationInfos = createApplicationInfos().also { Disposer.register(this, it) }
  val applications = createApplications().also { Disposer.register(this, it) }
  private val jobs = createJobs().also { Disposer.register(this, it) }
  private val stages = createStages().also { Disposer.register(this, it) }
  private val tasks = createTasks().also { Disposer.register(this, it) }
  private val sqls = createSqls().also { Disposer.register(this, it) }
  private val executors = createExecutors().also { Disposer.register(this, it) }
  private val storages = createStorages().also { Disposer.register(this, it) }
  private val tasksSummary = createTasksSummaries().also { Disposer.register(this, it) }
  private val executorsAggregateModels = createExecutorsAggInfos().also { Disposer.register(this, it) }
  private val environments = createEnvironments().also { Disposer.register(this, it) }


  init {
    init()
    RootDataModelStorage(updater, listOf(applications)).also { Disposer.register(this, it) }
  }

  fun getJobsModel(applicationId: AppAttemptId) = jobs[applicationId]

  fun getStagesModel(id: StagesDataId) = stages[id]

  fun getApplication(applicationId: AppAttemptId) = applications.data?.firstOrNull { it.id == applicationId }

  fun getTasksSummaryModel(applicationId: AppAttemptId, stageId: Int, attemptId: Int): ObjectDataModel<Metric> =
    tasksSummary[TasksSummaryDataId(applicationId, stageId, attemptId)]

  /** Returns model for the specific tasks list of the selected attempt of selected stage*/
  fun getTasksModel(applicationId: AppAttemptId, stageId: Int, attemptId: Int) =
    tasks[TasksDataId(applicationId, stageId, attemptId)]

  fun getSqlModel(applicationId: AppAttemptId) = sqls[applicationId]

  fun getExecutorsModel(applicationId: AppAttemptId) = executors[applicationId]

  fun getExecutorsAggregateModel(applicationId: AppAttemptId) = executorsAggregateModels[applicationId]

  fun getStoragesModel(applicationId: AppAttemptId) = storages[applicationId]

  fun getEnvironmentModel(applicationId: AppAttemptId) = environments[applicationId]

  private fun createProperties(entries: List<List<String>>): FieldsDataModel {
    val entriesPairs = entries.filter { it.size == 2 }.map { it[0] to it[1] }
    return FieldsDataModel.createForList(entriesPairs)
  }

  private val ApplicationStatus.queryValue
    get() = when (this) {
      ApplicationStatus.COMPLETE -> "completed"
      ApplicationStatus.RUNNING -> "running"
      ApplicationStatus.STARTING -> null
      ApplicationStatus.ERROR -> null
      ApplicationStatus.UNKNOWN -> null
    }


  private fun createStorages() = ObjectDataModelStorage<AppAttemptId, RDDStorageInfo>(updater, RDDStorageInfo::id) {
    client.getStorages(it)
  }

  private fun createSqls() = ObjectDataModelStorage<AppAttemptId, SqlInfo>(updater, SqlInfo::id) {
    if (connectionData.historyServer)
      client.getHistorySql(it)
    else
      client.getSql()
  }

  private fun createTasks() = ObjectDataModelStorage<TasksDataId, TaskData>(updater, TaskData::id) {
    client.getTasks(it.applicationId, it.stageId, it.attemptId)
  }

  private fun createJobs() = ObjectDataModelStorage<AppAttemptId, JobData>(updater, JobData::id) { id ->
    client.getJobs(id)
  }

  private fun createStages() = ObjectDataModelStorage<StagesDataId, StageData>(updater, StageData::id) {
    val stageIds = it.stageIds ?: emptyList()
    client.getStages(it.applicationId).applyIf(stageIds.isNotEmpty()) {
      filter { stage -> stage.id.toString() in stageIds }
    }
  }

  private fun createTasksSummaries() = ObjectDataModelStorage<TasksSummaryDataId, Metric>(updater, Metric::metric) {
    val tasks = client.getTasks(it.applicationId, it.stageId, it.attemptId)

    val fieldsMap = TaskMetrics.fieldsMap

    // Creating array for each field in fieldsMap.
    val metrics = ArrayList<Pair<String, ArrayList<Long>>>(fieldsMap.size)

    fieldsMap.forEach { pair ->
      metrics.add(Pair(pair.first, ArrayList(tasks.size)))
    }

    for (taskInfo in tasks) {
      if (taskInfo.taskMetrics == null) {
        continue
      }

      for (fieldWithIndex in fieldsMap.withIndex()) {
        metrics[fieldWithIndex.index].second.add(fieldWithIndex.value.second(taskInfo.taskMetrics))
      }
    }

    metrics.forEach { metric -> metric.second.sort() }

    val newMetrics = ArrayList<Metric>(metrics.size)

    metrics.forEach { pair ->
      newMetrics.add(
        if (pair.second.isEmpty()) {
          Metric(pair.first, 0L, 0L, 0L, 0L, 0L)
        }
        else {
          Metric(pair.first, pair.second[0],
                 pair.second[(pair.second.size * 0.25).toInt()],
                 pair.second[(pair.second.size * 0.5).toInt()],
                 pair.second[(pair.second.size * 0.75).toInt()],
                 pair.second[pair.second.size - 1])
        })
    }
    newMetrics
  }

  private fun createExecutorsAggInfos() = ObjectDataModelStorage<AppAttemptId, ExecutorsAggregateInfo>(updater,
                                                                                                       ExecutorsAggregateInfo::name) {
    val sourceModel = getExecutorsModel(it)
    val executors = sourceModel.entries
    listOf(
      ExecutorsAggregateInfo.createAggregateFrom(executors, isActive = true),
      ExecutorsAggregateInfo.createAggregateFrom(executors, isActive = false),
      ExecutorsAggregateInfo.createAggregateFrom(executors, isActive = null)
    )
  }

  private fun createApplicationInfos() = FieldGroupsDataModelStorage(updater, applications) { id: AppAttemptId ->
    val appInfo = applications.data?.firstOrNull { it.id == id }
                  ?: return@FieldGroupsDataModelStorage FieldGroupsData.empty()

    val properties = RemoteInfo.getProperties(appInfo).filter { it.name != PresentableApplicationInfo::byMe.name }
    val dataModel = FieldsDataModel(properties.map { it.name to it.getter.call(appInfo) }, appInfo).also { it.update() }
    FieldGroupsData(appInfo, listOf(
      SMMessagesBundle.message("title.summary") to dataModel,
    ))
  }


  private fun createApplications() = ObjectDataModel(PresentableApplicationInfo::id) { dataModel ->
    val toolwindowSettings = SparkToolwindowSettings.getInstance()
    val states = toolwindowSettings.getSparkConfigOrDefault(connectionData.innerId).applicationStatuses

    val sparkConfig = toolwindowSettings.getSparkConfigOrDefault(connectionData.innerId)
    val filters = dataModel.filters.getFilters()
    val limit: Int? = sparkConfig.applicationsLimit
    val minDate: String? = filters[PresentableApplicationInfo.STARTED_BEGIN_FILTER.name]?.toSparkDateFormat()
    val maxDate: String? = filters[PresentableApplicationInfo.STARTED_END_FILTER.name]?.toSparkDateFormat()
    val minEndDate: String? = filters[PresentableApplicationInfo.FINISHED_BEGIN_FILTER.name]?.toSparkDateFormat()
    val maxEndDate: String? = filters[PresentableApplicationInfo.FINISHED_END_FILTER.name]?.toSparkDateFormat()

    val appFilter = sparkConfig.appFilter.lowercase()
    val requestLimit = limit?.let { it * (if (appFilter.isBlank()) 1 else 4) }?.let { minOf(maxOf(1000, limit), it) }
    val serverApps = listAppsFromServer(states, requestLimit?.toString(), minDate, maxDate, minEndDate, maxEndDate)
    val notSyncApps = notSyncAppManager.getNotSyncApps(serverApps, states)


    val totalSortedApps = (serverApps + notSyncApps).sortedWith(compareBy({ it.status.importance }, { it.startTime?.time?.times(-1) }))


    val filteredNames = if (appFilter.isNotBlank())
      totalSortedApps.filter { it.toString().lowercase().contains(appFilter) }
    else
      totalSortedApps

    val filteredByMe = if (toolwindowSettings.getSparkConfigOrDefault(connectionData.innerId).showOnlyMyTasks)
      filteredNames.filter { it.byMe }
    else
      filteredNames

    val isFiltered = minDate != null || maxDate != null || minEndDate != null || maxEndDate != null || appFilter.isNotBlank() ||
                     !states.containsAll(setOf(ApplicationStatus.RUNNING, ApplicationStatus.COMPLETE)) ||
                     toolwindowSettings.getSparkConfigOrDefault(connectionData.innerId).showOnlyMyTasks ||
                     limit != null && serverApps.size >= limit
    filteredByMe to isFiltered
  }

  private fun listAppsFromServer(states: MutableSet<ApplicationStatus>,
                                 limit: String?,
                                 minDate: String?,
                                 maxDate: String?,
                                 minEndDate: String?,
                                 maxEndDate: String?): List<PresentableApplicationInfo> {
    val finalApps = mutableListOf<ApplicationInfo>()
    if (ApplicationStatus.RUNNING in states) {
      val running = client.getApplications(limit = limit,
                                           minDate = minDate,
                                           maxDate = maxDate,
                                           minEndDate = minEndDate,
                                           maxEndDate = maxEndDate,
                                           status = ApplicationStatus.RUNNING.queryValue)
      finalApps.addAll(running)
    }
    if (ApplicationStatus.COMPLETE in states) {
      val running = client.getApplications(limit = limit,
                                           minDate = minDate,
                                           maxDate = maxDate,
                                           minEndDate = minEndDate,
                                           maxEndDate = maxEndDate,
                                           status = ApplicationStatus.COMPLETE.queryValue)
      finalApps.addAll(running)
    }

    val apps = limit?.toIntOrNull()?.let { finalApps.take(it) } ?: finalApps

    val presentableApps = apps.flatMap { appInfo ->
      PresentableApplicationInfo.createFor(appInfo, this)
    }

    return presentableApps
  }

  private fun createExecutors() = ObjectDataModelStorage<AppAttemptId, ExecutorSummary>(updater, ExecutorSummary::id) {
    client.getExecutors(it)
  }

  private fun createEnvironments() = FieldGroupsDataModelStorage<AppAttemptId, ApplicationEnvironmentInfo>(updater) {
    val environmentInfo = client.getEnvironment(it)
    val classPathGroup = createProperties(environmentInfo.classpathEntries)
    val hadoopGroup = createProperties(environmentInfo.hadoopProperties)
    val sparkGroup = createProperties(environmentInfo.sparkProperties)
    val systemGroup = createProperties(environmentInfo.systemProperties)

    val runtimeEntries = listOf(
      listOf("javaHome", environmentInfo.runtime.javaHome),
      listOf("javaVersion", environmentInfo.runtime.javaVersion),
      listOf("scalaVersion", environmentInfo.runtime.scalaVersion)
    )
    val runtimeGroup = createProperties(runtimeEntries)

    FieldGroupsData(environmentInfo, listOf(
      SMMessagesBundle.message("title.classpath.entries") to classPathGroup,
      SMMessagesBundle.message("title.hadoopProperties") to hadoopGroup,
      SMMessagesBundle.message("title.sparkProperties") to sparkGroup,
      SMMessagesBundle.message("title.systemProperties") to systemGroup,
      SMMessagesBundle.message("title.runtime") to runtimeGroup
    ))
  }


  companion object {
    fun getInstance(connectionId: String, project: Project): SparkDataManager? =
      (DriverManager.getDriverById(project, connectionId) as? SparkMonitoringDriver)?.dataManager
  }
}

private fun String.toSparkDateFormat(): String? {
  val date = Date(toLong())
  val dateFormat = SimpleDateFormat("yyyy-MM-dd")
  return dateFormat.format(date)
}


interface DataId
data class SqlDataId(val applicationId: AppAttemptId) : DataId
data class JobsDataId(val applicationId: AppAttemptId) : DataId
data class StagesDataId(val applicationId: AppAttemptId, val stageIds: List<String>? = null) : DataId
data class TasksDataId(val applicationId: AppAttemptId, val stageId: Int, val attemptId: Int) : DataId
data class TasksSummaryDataId(val applicationId: AppAttemptId, val stageId: Int, val attemptId: Int) : DataId
data class StoragesDataId(val applicationId: AppAttemptId) : DataId
