package com.jetbrains.bigdatatools.flink.data

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.StringDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.StringDataModelStorage
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.table.renderers.SplitStringRenderer
import com.jetbrains.bigdatatools.flink.client.FlinkClient
import com.jetbrains.bigdatatools.flink.model.JarInfo
import com.jetbrains.bigdatatools.flink.model.JobCheckpoints
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsConfig
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsHistory
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsSummary
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsSummaryStatistics
import com.jetbrains.bigdatatools.flink.model.JobExceptionEntry
import com.jetbrains.bigdatatools.flink.model.JobExceptionType
import com.jetbrains.bigdatatools.flink.model.JobExecutionConfig
import com.jetbrains.bigdatatools.flink.model.JobInfo
import com.jetbrains.bigdatatools.flink.model.JobManagerConfig
import com.jetbrains.bigdatatools.flink.model.JobVertex
import com.jetbrains.bigdatatools.flink.model.LogFileInfo
import com.jetbrains.bigdatatools.flink.model.TaskManagerInfo
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData
import com.jetbrains.bigdatatools.flink.rfs.FlinkDriver
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle
import kotlin.reflect.full.declaredMemberProperties

class FlinkDataManager(project: Project?,
                       override val connectionData: FlinkConnectionData,
                       settings: IntervalUpdateSettings) : MonitoringDataManager(project, settings) {
  override val client = FlinkClient(project, connectionData, false)

  val jarsModel = createJarsDataModel().also { Disposer.register(this, it) }
  val jobsModel = createJobsDataModel().also { Disposer.register(this, it) }
  val taskManagersModel = createTaskManagersModel().also { Disposer.register(this, it) }
  val jobManagerConfigModel = createJobManagerConfigModel().also { Disposer.register(this, it) }
  val jobManagerLogsListModel = createJobManagerLogsListModel().also { Disposer.register(this, it) }
  private var jobManagerLogModel = createJobManagerLogs().also { Disposer.register(this, it) }
  private var jobMangerStdOutModel = createJobManagerStdOuts().also { Disposer.register(this, it) }

  private var jobOverviewModels = createJobOverviews().also { Disposer.register(this, it) }
  private var jobAllExceptionsModels = createJobExceptions().also { Disposer.register(this, it) }
  private var jobExceptionHistoryModels = createJobExceptionHistories().also { Disposer.register(this, it) }
  private var jobCheckpointsHistoryModel = createCheckpointHistories().also { Disposer.register(this, it) }
  private var jobCheckpointsSummaryModel = createCheckpointsSummaries().also { Disposer.register(this, it) }
  private var taskManagerLogsListModel = createTaskManangerLogs().also { Disposer.register(this, it) }
  private var jobCheckpointsOverviewModel = createJobCheckpoints().also { Disposer.register(this, it) }
  private var jobConfigModels = createJobConfigs().also { Disposer.register(this, it) }
  private var jobCheckpointsConfigModel = createJobCheckpointsConfigs().also { Disposer.register(this, it) }
  private var taskManagerLogModel = createTaskManagerLogs().also { Disposer.register(this, it) }
  private var taskMangerStdOutModel = createTaskManagerStdOuts().also { Disposer.register(this, it) }
  private var taskMangerThreadDumpModel = createTaskManagerDumps().also { Disposer.register(this, it) }
  private var taskManagerLogFileModel = createTaskManagerLogFiles().also { Disposer.register(this, it) }
  private var jobManagerLogFileModel = createJobManagerLogFiles().also { Disposer.register(this, it) }


  init {
    init()
    //Exclude jobManagerLogModel, jobMangerStdOutModel because they don't need to be in auto-update
    val rootModelsForUpdate = listOf(jarsModel, jobsModel, taskManagersModel, jobManagerConfigModel, jobManagerLogsListModel)
    RootDataModelStorage(updater, rootModelsForUpdate).also { Disposer.register(this, it) }
  }

  fun getJobOverviewModel(id: String) = jobOverviewModels[id]
  fun getJobAllExceptionsModel(id: String) = jobAllExceptionsModels[id]
  fun getJobExceptionHistoryModel(id: String) = jobExceptionHistoryModels[id]

  fun getJobCheckpointsOverviewModel(id: String) = jobCheckpointsOverviewModel[id]
  fun getJobCheckpointsHistoryModel(id: String) = jobCheckpointsHistoryModel[id]
  fun getJobCheckpointsSummaryModel(id: String) = jobCheckpointsSummaryModel[id]
  fun getJobCheckpointsConfigModel(id: String) = jobCheckpointsConfigModel[id]
  fun getJobConfigModel(id: String) = jobConfigModels[id]

  private fun createTaskManagersModel() = ObjectDataModel(TaskManagerInfo::id) {
    client.getTaskManagers() to false
  }

  fun getTaskManagerLogModel(id: String) = taskManagerLogModel[id]


  fun getTaskManagerStdOutModel(id: String) = taskMangerStdOutModel[id]

  fun getTaskManagerLogListModel(id: String) = taskManagerLogsListModel[id]

  fun getTaskManagersLogFileModel(taskManagerId: String, fileId: String, isNeededToUpdate: Boolean = false): StringDataModel {
    val stringDataModel = taskManagerLogFileModel[TaskFileId(taskManagerId, fileId)]
    if (isNeededToUpdate)
      updater.invokeRefreshModel(stringDataModel)
    return stringDataModel
  }

  fun getTaskManagerThreadDumpModel(id: String, isNeededToUpdate: Boolean = false): StringDataModel {
    val dataModel = taskMangerThreadDumpModel[id]
    if (isNeededToUpdate) {
      updater.invokeRefreshModel(dataModel)
    }
    return dataModel
  }

  private fun createJobManagerConfigModel() = ObjectDataModel(JobManagerConfig::key) {
    client.getJobManagerConfig() to false
  }

  fun getJobManagerLogModel(): StringDataModel = jobManagerLogModel

  fun getJobMangerStdOutModel(): StringDataModel = jobMangerStdOutModel

  private fun createJobManagerLogsListModel() = ObjectDataModel(LogFileInfo::name) {
    client.getJobMangerLogList() to false
  }

  fun getJobManagerLogFileModel(fileName: String) = jobManagerLogFileModel[fileName]

  fun uploadJar(pathToJarFile: String) = actionWrapper {
    client.uploadJar(pathToJarFile)
    updater.invokeRefreshModel(jarsModel)
  }

  fun removeJar(jar: JarInfo) = actionWrapper {
    client.deleteJar(jar.id)
    updater.invokeRefreshModel(jarsModel)
  }

  fun runJar(jarId: String, data: Map<String, Any?>) = actionWrapper {
    client.runJar(jarId, data)
    updater.invokeRefreshModel(jobsModel)
  }

  fun cancelJob(jobId: String) = actionWrapper {
    client.cancelJob(jobId)
    updater.invokeRefreshModel(jobsModel)
  }

  private fun actionWrapper(body: () -> Unit) = executeOnPooledThread {
    try {
      body()
    }
    catch (t: Throwable) {
      NotificationUtils.notifyException(t,
                                           FlinkMessagesBundle.message("flink.error"),
                                           FlinkMessagesBundle.message("flink.error.details.dialog")
      )
    }
  }

  private fun createJobOverviews() = ObjectDataModelStorage<String, JobVertex>(updater, JobVertex::id) {
    client.getDetailsOfJob(it).vertices
  }

  private fun createJobExceptions() = ObjectDataModelStorage<String, JobExceptionType>(updater, JobExceptionType::exception) {
    client.getJobException(it).allException
  }

  private fun createJobExceptionHistories() = ObjectDataModelStorage<String, JobExceptionEntry>(updater, JobExceptionEntry::exceptionName) {
    client.getJobException(it).exceptionHistory.entries
  }

  private fun createCheckpointHistories() = ObjectDataModelStorage<String, JobCheckpointsHistory>(updater, JobCheckpointsHistory::id) {
    client.getJobCheckpoints(it)?.history ?: emptyList()
  }

  private fun createCheckpointsSummaries() = ObjectDataModelStorage<String, JobCheckpointsSummaryStatistics>(
    updater,
    JobCheckpointsSummaryStatistics::type) {
    val summary = client.getJobCheckpoints(it)?.summary

    if (summary == null)
      emptyList()
    else {
      val newValue = mutableListOf<JobCheckpointsSummaryStatistics>()
      val declaredProperties = JobCheckpointsSummary::class.declaredMemberProperties
      for (property in declaredProperties) {
        val value = property.get(summary)
        if (value is JobCheckpointsSummaryStatistics) {
          value.type = SplitStringRenderer.camelCaseToReadable(property.name)
          newValue.add(value)
        }
      }
      newValue
    }
  }

  private fun createTaskManangerLogs() = ObjectDataModelStorage<String, LogFileInfo>(updater, LogFileInfo::name) {
    client.getTaskManagerLogList(it)
  }

  private fun createJarsDataModel() = ObjectDataModel(JarInfo::id) {
    val settings = FlinkToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(connectionData.innerId)

    client.getJars(config.jarFilter, config.jarLimit) to false
  }

  private fun createJobsDataModel() = ObjectDataModel(JobInfo::jid) {
    val settings = FlinkToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(connectionData.innerId)

    client.getJobs(settings.jobStatus, config.jobFilter, config.jobLimit) to false
  }

  private fun createJobConfigs() = FieldGroupsDataModelStorage<String, JobExecutionConfig>(updater) {
    val newValue = client.getJobConfig(it)
    FieldGroupsData(newValue, listOf(
      FlinkMessagesBundle.message("datamanager.executionConfiguration") to FieldsDataModel.createForObject(newValue),
    ))
  }

  private fun createJobCheckpointsConfigs() = FieldGroupsDataModelStorage<String, JobCheckpointsConfig>(updater) {
    val newValue = client.getJobCheckpointsConfig(it) ?: return@FieldGroupsDataModelStorage FieldGroupsData.empty()
    FieldGroupsData(newValue, listOf(
      FlinkMessagesBundle.message("datamanager.checkpointsConfiguration") to FieldsDataModel.createForObject(newValue),
    ))
  }

  private fun createJobCheckpoints() = FieldGroupsDataModelStorage<String, JobCheckpoints>(updater) {
    val newValue = client.getJobCheckpoints(it) ?: return@FieldGroupsDataModelStorage FieldGroupsData.empty<JobCheckpoints>()
    FieldGroupsData(newValue, listOf(
      FlinkMessagesBundle.message("datamanager.checkpointsCounts") to FieldsDataModel.createForObject(newValue.counts),
      FlinkMessagesBundle.message("datamanager.latestCheckpoint") to FieldsDataModel.createForObject(newValue.latest),
    ))
  }

  private fun createTaskManagerLogs() = StringDataModelStorage<String>(updater) {
    client.getTaskManagerLog(it)
  }

  private fun createTaskManagerStdOuts() = StringDataModelStorage<String>(updater) {
    client.getTaskManagerStdOut(it)
  }

  private fun createTaskManagerDumps() = StringDataModelStorage<String>(updater) {
    client.getTaskManagerThreadDump(it)
  }

  private fun createTaskManagerLogFiles() = StringDataModelStorage<TaskFileId>(updater) {
    client.getTaskManagerLogFile(it.taskManagerId, it.fileId)
  }

  private fun createJobManagerLogFiles() = StringDataModelStorage<String>(updater) {
    client.getJobManagerLogFile(it)
  }

  private fun createJobManagerLogs() = StringDataModel {
    client.getJobManagerLog()
  }

  private fun createJobManagerStdOuts() = StringDataModel {
    client.getJobMangerStdOut()
  }

  companion object {
    data class TaskFileId(val taskManagerId: String, val fileId: String)

    fun getInstance(connectionId: String, project: Project) =
      (DriverManager.getDriverById(project, connectionId) as? FlinkDriver)?.dataManager
  }
}