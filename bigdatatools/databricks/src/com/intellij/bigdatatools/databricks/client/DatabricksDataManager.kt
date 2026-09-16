package com.intellij.bigdatatools.databricks.client

import com.databricks.sdk.service.jobs.SubmitRun
import com.databricks.sdk.service.jobs.SubmitRunResponse
import com.databricks.sdk.service.workspace.Import
import com.databricks.sdk.service.workspace.ImportFormat
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.databricks.model.ClusterConfiguration
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.model.ServerRunInfo
import com.intellij.bigdatatools.databricks.model.WorkflowRunInfo
import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.serverRunPath
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.workflowRunPath
import com.intellij.bigdatatools.databricks.run.direct.DbServerExecutionStorage
import com.intellij.bigdatatools.databricks.run.workflow.DbWorkflowExecutionStorage
import com.intellij.bigdatatools.databricks.sync.DatabricksSyncManager
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import kotlinx.coroutines.delay
import java.util.Base64

internal class DatabricksDataManager(project: Project?,
                            override val connectionData: DatabricksConnectionData,
                            settings: IntervalUpdateSettings) : MonitoringDataManager(project, settings) {

  val connectionId = connectionData.innerId
  override val client = DatabricksClient(project, connectionData)

  val serverExecutionStorage = DbServerExecutionStorage(this)
  val workflowExecutionStorage = DbWorkflowExecutionStorage(this)

  private val serverRunDetailsModel = FieldGroupsDataModelStorage<String, ServerRunInfo>(updater) { contextId ->
    val serverRunInfo = serverExecutionStorage.getInfo(contextId)
                        ?: error(DatabricksBundle.message("error.cannot.find.execution.with.context.id", contextId))
    FieldGroupsData(serverRunInfo, emptyList())
  }.apply {
    Disposer.register(this@DatabricksDataManager, this)
  }

  private val workflowDetailsModels = FieldGroupsDataModelStorage<Long, WorkflowRunInfo>(updater) { runId ->
    val runInfo = workflowExecutionStorage.getInfo(runId)
                  ?: error(DatabricksBundle.message("error.cannot.find.workflow.execution.with.context.id", runId))
    FieldGroupsData(runInfo, emptyList())
  }.apply {
    Disposer.register(this@DatabricksDataManager, this)
  }

  val configurationModel = FieldsGroupModel {
    val user = client.getUser()
    val clusters = client.getClusters()
    val info = ClusterConfiguration(user = user, clusters = clusters)
    FieldGroupsData(info, listOf())
  }

  val syncManager = DatabricksSyncManager(this).also { Disposer.register(this, it) }

  init {
    init()
    RootDataModelStorage(updater, listOf(configurationModel)).also { Disposer.register(this, it) }
  }

  fun getCurrentUser(): String? = configurationModel.data?.obj?.user

  fun startCluster(cluster: ClusterInfoPresentable) {
    client.startCluster(cluster.id)
    updater.invokeRefreshModel(configurationModel)
    NotificationUtils.notifySuccess(DatabricksBundle.message("notification.content.cluster.start", cluster.name),
                                    DatabricksBundle.message("notification.action.title"))
  }

  suspend fun waitClusterStart(cluster: ClusterInfoPresentable): Boolean {
    while (true) {
      val clusterInfo = loadCluster(cluster.id) ?: error("Cannot get cluster info")
      val isFinished = clusterInfo.isClusterReady()
      if (isFinished) {
        updater.syncRefreshModels(listOf(configurationModel))
        return true
      }

      // If we have an error or termination state, we should return.
      if (cluster.isClusterStopped()) {
        updater.syncRefreshModels(listOf(configurationModel))
        return false
      }

      ProgressManager.progress2(DatabricksBundle.message("progress.2.cluster.status", clusterInfo.info.state?.name ?: ""))
      delay(2000)
    }
  }

  fun restartCluster(cluster: ClusterInfoPresentable) = actionWrapper {
    client.restartCluster(cluster.id)
    updater.invokeRefreshModel(configurationModel)
  }

  fun stopCluster(cluster: ClusterInfoPresentable) = actionWrapper {
    client.stopCluster(cluster.id)
    updater.invokeRefreshModel(configurationModel)
    NotificationUtils.notifySuccess(DatabricksBundle.message("notification.content.cluster.stop", cluster.name),
                                       DatabricksBundle.message("notification.action.title"))
  }

  fun getWorkflowRun(runId: Long): WorkflowRunInfo? = workflowExecutionStorage.getInfo(runId)

  fun getServerRun(contextId: String): ServerRunInfo? = serverExecutionStorage.getInfo(contextId)

  fun getClusters() = configurationModel.data?.obj?.clusters ?: emptyList()

  private fun actionWrapper(body: () -> Unit) = executeOnPooledThread {
    actionWrapperSync(body)
  }

  private fun actionWrapperSync(body: () -> Unit) = try {
    body()
  }
  catch (t: Throwable) {
    NotificationUtils.notifyException(t, DatabricksBundle.message("datamodel.error"))
  }

  fun changeAttachedCluster(cluster: ClusterInfoPresentable?) {
    connectionData.connectedClusterId = cluster?.id ?: ""
    updater.invokeRefreshModel(configurationModel)
  }

  fun getWorkflowRunModel(runId: Long) = workflowDetailsModels[runId]
  fun getServerRunModel(contextId: String) = serverRunDetailsModel[contextId]

  fun getCurrentCluster(): ClusterInfoPresentable? {
    if (!client.isConnected()) {
      return null
    }

    val clusterId = connectionData.connectedClusterId
    return getClusters().firstOrNull { it.id == clusterId } ?: loadCluster(clusterId)
  }

  fun loadCluster(clusterId: String) = client.getCluster(clusterId)

  fun saveTextFile(rfsPath: RfsPath, text: String, format: ImportFormat) {
    val request = Import()
    val encoded = Base64.getEncoder().encodeToString(text.toByteArray())
    request.content = encoded
    request.path = rfsPath.stringRepresentation()
    request.overwrite = true
    request.format = format
    client.workspaceImport(request)
  }

  fun refreshWorkspaceExecutions(runId: Long) {
    val runModel = getWorkflowRunModel(runId)
    runModel.update()
    driver.fileInfoManager.refreshFiles(workflowRunPath)
  }

  fun refreshServerExecutions(contextId: String) {
    val runModel = getServerRunModel(contextId)
    runModel.update()
    driver.fileInfoManager.refreshFiles(serverRunPath)
  }

  fun runJob(project: Project, runParameters: SubmitRun): SubmitRunResponse {
    val submitJob = client.submitJob(runParameters)

    //val createRunConfigurationAction = DumbAwareAction.create(DatabricksBundle.message("notification.action.create.run.configuration")) {
    //  val allTypes = ConfigurationType.CONFIGURATION_TYPE_EP.extensionList
    //  val configurationType = allTypes.firstOrNull { it.id == DatabricksRunConfigurationType.DATABRICKS_ID } ?: return@create
    //  val configuration = ProjectRunConfigurationConfigurable(project).createNewConfiguration(configurationType.configurationFactories.first())
    //
    //  (configuration.configuration as? DatabricksRunConfiguration)?.apply {
    //    configurationId = connectionId
    //    clusterId = connectionData.connectedClusterId
    //    filePath = file.path
    //    // ToDo we need to provide mode from DatabricksWorkflowRunner / DatabricksServerRunner.
    //    mode = if ("ipynb" == file.extension?.lowercase()) DatabricksRunMode.WORKFLOW else DatabricksRunMode.SERVER
    //  }
    //}

    return submitJob
  }

  companion object {
    fun getInstance(connectionId: String, project: Project): DatabricksDataManager? =
      (DriverManager.getDriverById(project, connectionId) as? DatabricksDriver)?.dataManager
  }
}