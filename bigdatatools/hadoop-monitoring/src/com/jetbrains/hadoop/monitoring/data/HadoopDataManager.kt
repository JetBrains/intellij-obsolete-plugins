package com.jetbrains.hadoop.monitoring.data

import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.StringDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import com.jetbrains.hadoop.monitoring.data.models.DataModelFs
import com.jetbrains.hadoop.monitoring.data.models.FsContentListener
import com.jetbrains.hadoop.monitoring.data.models.FsListListener
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.HadoopResourceManagerRestClient
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppAttemptInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ContainerInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.HadoopConfigurationProperty
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.LogFileInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeLabel
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.YarnApplicationState
import com.jetbrains.hadoop.monitoring.rfs.driver.HadoopMonitoringDriver
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.statistics.HadoopMonitoringUsagesCollector
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import com.jetbrains.hadoop.monitoring.util.loadFilters
import org.jetbrains.concurrency.AsyncPromise

/** Manages all data for selected configuration. This could be applications or cluster info. */
class HadoopDataManager(project: Project?,
                        override val connectionData: HadoopConnectionData) : MonitoringDataManager(project,
                                                                                                   HadoopSettings.getInstance()) {
  override val client: HadoopResourceManagerRestClient = HadoopResourceManagerRestClient(project, connectionData, isTest = false)
  private val toolsLogs = DataModelFs(
    requestList = { name ->
      client.getLogsList(name).filter { it.name != "Parent Directory" }
    },
    requestContent = {
      client.getLogFileContent(it)
    })


  private val applications = createApplicationModel(project).also { Disposer.register(this, it) }
  private val nodes = createNodes(project).also { Disposer.register(this, it) }
  private val clusterInfo = createClusterInfos().also { Disposer.register(this, it) }
  private val toolsConfiguration = createToolConfigs().also { Disposer.register(this, it) }
  private val toolsMetrics = createToolMetrics().also { Disposer.register(this, it) }
  private val toolsStacks = createToolStacks().also { Disposer.register(this, it) }
  private val nodeLabels = createNodeLabels(project).also { Disposer.register(this, it) }
  private val containersModels = createContainersStorage().also { Disposer.register(this, it) }
  private val appAttemptsModels = createAppAttemptsStorage(project).also { Disposer.register(this, it) }

  init {
    init()

    toolsLogs.addContentListener(object : FsContentListener {
      override fun onFileContentUpdate(path: String, content: String) {
        HadoopMonitoringUsagesCollector.logFileOpenedEvent.log(project, content.length)
      }

      override fun onError(msg: String, t: Throwable) {}
    })

    toolsLogs.addListListener(object : FsListListener {
      override fun onListUpdate(path: String, list: List<LogFileInfo>) {
        HadoopMonitoringUsagesCollector.logListReceivedEvent.log(project, list.size)
      }

      override fun onError(msg: String, t: Throwable) {}
    })

    RootDataModelStorage(updater, listOf(applications, nodes, clusterInfo,
                                         toolsConfiguration, toolsMetrics, toolsStacks, nodeLabels)).also { Disposer.register(this, it) }
  }

  fun killApp(appId: String): AsyncPromise<YarnApplicationState> {
    val promise = BdtAsyncPromise<YarnApplicationState>()
    executeOnPooledThread {
      try {
        HadoopMonitoringUsagesCollector.killEvent.log(project)
        promise.setResult(client.updateAppState(appId, YarnApplicationState.KILLED))
      }
      catch (t: Throwable) {
        promise.setError(t)
      }
    }
    return promise
  }

  fun getContainersInfo(applicationId: String, attemptId: Int) = containersModels[AppAttemptId(applicationId, attemptId)]

  fun getApplicationAttemptsModel(applicationId: String) = appAttemptsModels[applicationId]

  fun getToolsConfiguration() = toolsConfiguration
  fun getToolsLogs() = toolsLogs
  fun getToolsMetrics() = toolsMetrics
  fun getToolsStacks() = toolsStacks
  fun getClusterInfo() = clusterInfo
  fun getApplicationsModel(): ObjectDataModel<AppInfo> = applications
  fun getNodesModel(): ObjectDataModel<NodeInfo> = nodes
  fun getNodeLabelsModel(): ObjectDataModel<NodeLabel> = nodeLabels

  private fun validateDates(begin: String?, end: String?, type: String) {
    val beginMills = begin?.toLong() ?: return
    val endMills = end?.toLong() ?: return
    if (beginMills > endMills) {
      error("$type date begin must be less than end.")
    }
  }

  private fun createContainersStorage() = ObjectDataModelStorage<AppAttemptId, ContainerInfo>(updater, ContainerInfo::containerId) {
    client.getContainers(it.applicationId, it.attemptId)
  }

  private fun createAppAttemptsStorage(project: Project?) =
    ObjectDataModelStorage<String, AppAttemptInfo>(updater, AppAttemptInfo::id) { applicationId ->
      val newValue = client.getAppAttempts(applicationId)
      HadoopMonitoringUsagesCollector.appAttemptsReceivedEvent.log(project, newValue.size)
      newValue
    }

  private fun createApplicationModel(project: Project?) = ObjectDataModel(AppInfo::id) {
    it.filters.loadFilters(connectionData.innerId)

    val filters = it.filters.getFilters()
    val limit = filters["limit"]?.toInt()
    val startedBegin = filters["startedBegin"]
    val startedEnd = filters["startedEnd"]
    val finishBegin = filters["finishBegin"]
    val finishEnd = filters["finishEnd"]
    val statesQuery = filters["statesQuery"]
    val user = filters["userQuery"]

    validateDates(startedBegin, startedEnd, "Started")
    validateDates(finishBegin, finishEnd, "Finished")
    val newData = client.getApps(limit = limit,
                                 startedBegin = startedBegin,
                                 startedEnd = startedEnd,
                                 finishBegin = finishBegin,
                                 finishEnd = finishEnd,
                                 statesQuery = statesQuery,
                                 user = user)
    if (it.isInitedByFirstTime) {
      HadoopMonitoringUsagesCollector.appsReceivedEvent.log(project, newData.size)
    }
    newData to false
  }

  private fun createNodes(project: Project?) = ObjectDataModel(NodeInfo::id) {
    val newValue = client.getNodes()
    if (it.isInitedByFirstTime)
      HadoopMonitoringUsagesCollector.nodesReceivedEvent.log(project, newValue.size)
    newValue to false
  }

  private fun createClusterInfos() = FieldsGroupModel {
    val info = client.getClusterInfo()
    val clusterMetricsInfo = client.getClusterMetricsInfo()
    val schedulerInfo = client.getSchedulerInfo().schedulerInfo

    FieldGroupsData(info, listOf(
      HadoopMessagesBundle.message("title.clusterInfo") to FieldsDataModel.createForObject(info),
      HadoopMessagesBundle.message("title.clusterMetricsInfo") to FieldsDataModel.createForObject(clusterMetricsInfo),
      HadoopMessagesBundle.message("title.schedulerInfo") to FieldsDataModel.createForObject(schedulerInfo))
    )
  }

  private fun createToolConfigs() = ObjectDataModel(HadoopConfigurationProperty::name) {
    client.getToolsConfiguration() to false
  }

  private fun createToolMetrics() = StringDataModel {
    client.getToolsMetrics()
  }

  private fun createToolStacks() = StringDataModel {
    client.getToolsStacks()
  }

  private fun createNodeLabels(project: Project?) = ObjectDataModel(NodeLabel::labelName) {
    val newValue = client.getNodeLabels()
    if (!it.isInitedByFirstTime) {
      HadoopMonitoringUsagesCollector.nodesLabelsReceivedEvent.log(project, newValue.size)
    }
    newValue to false
  }


  companion object {
    data class AppAttemptId(val applicationId: String, val attemptId: Int)

    fun getInstance(connectionId: String, project: Project): HadoopDataManager? =
      (DriverManager.getDriverById(project, connectionId) as? HadoopMonitoringDriver)?.dataManager
  }
}