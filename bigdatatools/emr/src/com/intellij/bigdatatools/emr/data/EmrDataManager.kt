package com.intellij.bigdatatools.emr.data

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.emr.client.BdtEmrClient
import com.intellij.bigdatatools.emr.dependend.EmrDependsManager
import com.intellij.bigdatatools.emr.dependend.EmrSshManager
import com.intellij.bigdatatools.emr.model.EmrClusterAppInfo
import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.model.EmrClusterInstanceInfo
import com.intellij.bigdatatools.emr.model.EmrClusterState
import com.intellij.bigdatatools.emr.model.EmrClusterStepInfo
import com.intellij.bigdatatools.emr.rfs.EmrDriver
import com.intellij.bigdatatools.emr.settings.EmrConnectionData
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.toolwindow.controllers.isRunning
import com.intellij.bigdatatools.emr.util.EmrClusterAppUtils
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.use
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.SystemNotifications
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.updater.WaitConditionTask
import com.jetbrains.bigdatatools.common.rfs.copypaste.RfsCopyPasteManager
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.rfs.driver.refreshConnectionBlocking
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.common.util.launchForegroundTask
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import software.amazon.awssdk.services.emr.model.ClusterSummary
import software.amazon.awssdk.services.emr.model.InstanceGroupType
import software.amazon.awssdk.services.emr.model.StepConfig
import software.amazon.awssdk.services.emr.model.StepSummary
import java.io.FileNotFoundException
import java.util.zip.GZIPInputStream

class EmrDataManager(project: Project?,
                     override val connectionData: EmrConnectionData,
                     override val settings: EmrToolWindowSettings) : MonitoringDataManager(project, settings) {
  val sshManager = EmrSshManager(this)

  val dependsManager = EmrDependsManager(this).also {
    Disposer.register(this, it)
  }

  val region = connectionData.region

  override val client = BdtEmrClient(project, connectionData)

  val clusterModel = createClusterModel().also { Disposer.register(this, it) }
  private val stepsModels = createStepModel().also { Disposer.register(this, it) }
  private val appsModels = createClusterApps().also { Disposer.register(this, it) }
  private val clusterInstancesModels = createClusterInstances().also { Disposer.register(this, it) }
  private val clusterInfoModels = createClusterInfos().also { Disposer.register(this, it) }
  private val clusterStepInfoModels = createClusterSteps().also { Disposer.register(this, it) }


  init {
    init()
    RootDataModelStorage(updater, listOf(clusterModel)).also { Disposer.register(this, it) }
  }

  fun isClusterRun(clusterId: String?) = getClusterById(clusterId)?.state?.isRunning() == true

  fun getClusterById(id: String?) = clusterModel.entries.firstOrNull { it.id == id }
  fun loadClusterById(id: String) = clusterInfoModels[id].originObject ?: client.getClusterDetails(id).let {
    EmrClusterDetails.fromCluster(it.cluster())
  }

  fun getClusterAppsDataModel(id: String) = appsModels[id]
  fun getStepsDataModel(id: String) = stepsModels[id]
  fun getClusterInstancesModel(id: String) = clusterInstancesModels[id]
  fun getStepInfoModel(clusterId: String, stepId: String) = clusterStepInfoModels[StepId(clusterId, stepId)]
  fun getClusterInfoModel(id: String): FieldsGroupModel<EmrClusterDetails> = clusterInfoModels[id]

  fun terminateCluster(id: String) {
    client.terminateCluster(id)
    this.updater.invokeRefreshModel(clusterModel)
    this.updater.invokeRefreshModel(getClusterInfoModel(id))
  }

  fun cancelSteps(clusterId: String, stepsIds: List<String>) = actionWrapper {
    client.cancelSteps(clusterId, stepsIds)
    this.updater.invokeRefreshModel(clusterModel)
    this.updater.invokeRefreshModel(getStepsDataModel(clusterId))
  }

  fun addStep(clusterId: String, stepConfig: StepConfig) = actionWrapper {
    val ids = client.addSteps(clusterId, stepConfig)
    this.updater.invokeRefreshModel(clusterModel)
    this.updater.invokeRefreshModel(getStepsDataModel(clusterId))
    ids.forEach { stepId ->
      WaitConditionTask.start(this, checkIsFinished = {
        if (client.isConnected())
          client.checkIsStepFinished(clusterId, stepId)
        else
          false
      }, onFinish = {
        this.updater.invokeRefreshModel(clusterModel)
        this.updater.invokeRefreshModel(getStepsDataModel(clusterId))
        this.updater.invokeRefreshModel(getStepInfoModel(clusterId, stepId))

        val step = client.getStepDetails(clusterId, stepId)

        @Suppress("DialogTitleCapitalization")
        val title = EmrMessagesBundle.message("cell.execution.finished.title")
        val stepName = step?.name()?.let { "$it ($stepId)" } ?: stepId
        val msg = EmrMessagesBundle.message("cell.execution.finished.msg", stepName,
                                            step?.status()?.stateAsString() ?: "<Unknown>")

        SystemNotifications.getInstance().notify(NotificationGroupManager.getInstance().getNotificationGroup("Job Notification").displayId,
                                                 title, msg)
        NotificationUtils.notifySuccess(msg, title, project = project)
      })
    }
  }

  fun setTerminationProtection(clusterId: String, enable: Boolean) = actionWrapper {
    client.setTerminationProtection(clusterId, enable)
    updater.invokeRefreshModel(getClusterInfoModel(clusterId))
  }

  fun getClusterDetailsOrLoad(clusterId: String): EmrClusterDetails =
    clusterInfoModels[clusterId].originObject ?: let {
      updater.syncRefreshModels(listOf(clusterInfoModels[clusterId]))
      clusterInfoModels[clusterId].originObject ?: error(EmrMessagesBundle.message("cluster.details.not.found", clusterId))
    }


  fun getClusterDetails(clusterId: String) =
    clusterInfoModels[clusterId].originObject ?: error(EmrMessagesBundle.message("cluster.details.not.found", clusterId))

  fun getCachedClusterDetails(clusterId: String?) = clusterModel.entries.firstOrNull { it.id == clusterId }


  private fun getClusterMasterInstance(id: String) = getClusterInstancesModel(id).entries.firstOrNull {
    it.type == InstanceGroupType.MASTER
  }

  private fun createClusterModel() = ObjectDataModel(EmrClusterInfo::id) {
    val config = settings.getOrCreateConfig(connectionData.innerId)
    client.getClusters(limit = config.clusterLimit, filter = config.clusterFilter,
                       states = settings.customClusterStates).map { EmrClusterInfo.getFrom(it) } to false
  }

  private fun createStepModel() = ObjectDataModelStorage<String, EmrClusterStepInfo>(updater, EmrClusterStepInfo::id) { id ->
    val config = settings.getOrCreateConfig(connectionData.innerId)
    client.getSteps(clusterId = id, limit = config.stepLimit, filter = config.stepFilter,
                    states = settings.stepStates).map { EmrClusterStepInfo.getFrom(it) }
  }


  private fun createClusterApps() = ObjectDataModelStorage<String, EmrClusterAppInfo>(updater, EmrClusterAppInfo::name) { id ->
    val clusterResult = client.getClusterDetails(id)
    val cluster = clusterResult.cluster()
    val apps = cluster.applications()
    apps.flatMap { EmrClusterAppUtils.getFor(it, EmrClusterDetails.fromCluster(cluster)) }
  }


  private fun createClusterInstances() = ObjectDataModelStorage<String, EmrClusterInstanceInfo>(updater, EmrClusterInstanceInfo::id) { id ->
    val config = settings.getOrCreateConfig(connectionData.innerId)
    client.getClusterInstances(
      id = id,
      limit = config.instanceLimit,
      filter = config.instanceFilter,
      filteredTypes = settings.instanceGroupTypes,
      states = settings.instanceStates)
  }


  private fun createClusterInfos() = FieldGroupsDataModelStorage<String, EmrClusterDetails>(updater) {
    val clusterResult = client.getClusterDetails(it)
    val clusterDetails = EmrClusterDetails.fromCluster(clusterResult.cluster())
    FieldGroupsData(clusterDetails, listOf(
      EmrMessagesBundle.message("title.summary") to FieldsDataModel.createForObject(clusterDetails.summary),
      EmrMessagesBundle.message("title.configuration") to FieldsDataModel.createForObject(clusterDetails.configuration),
      EmrMessagesBundle.message("title.network") to FieldsDataModel.createForObject(clusterDetails.network)
    ))
  }


  private fun createClusterSteps() = FieldGroupsDataModelStorage<StepId, StepSummary>(updater) { clusterStepId ->
    val clusterId = clusterStepId.clusterId
    val stepId = clusterStepId.stepId
    val source = getStepsDataModel(clusterId).data ?: emptyList()
    val clusterStepInfo = source.first { it.id == stepId }
    val clusterInfo = getClusterInfoModel(clusterId).originObject
    val fieldsDataModel = EmrDataManagerUtils.createInfoForStep(clusterStepInfo, clusterInfo)
    FieldGroupsData(clusterStepInfo.originalObject, listOf(EmrMessagesBundle.message("title.summary") to fieldsDataModel))
  }


  fun openLogFile(project: Project, clusterId: String, s3Uri: String) = launchForegroundTask(project, EmrMessagesBundle.message(
    "step.info.open.log.task.title"), true) { indicator ->
    try {
      val cluster = getClusterDetails(clusterId)
      val logFolderUri = clearS3Prefixes(cluster.cluster.logUri())
      val logFileUri = clearS3Prefixes(s3Uri)

      val s3ConnectionData = dependsManager.createS3ConnectionData(logFolderUri)
      val driver = s3ConnectionData.createDriver(project, isTest = true)
      val rfsPath = driver.createRfsPath(clearS3Prefixes(logFileUri))
      driver.use {
        driver.refreshConnectionBlocking(ActivitySource.EMR_DEPENDENT_USER).getException()?.let { throw it }
        val tempZipFile = FileUtil.createTempFile("aws-emr-$clusterId", "log-zip", true)
        RfsCopyPasteManager.downloadFromRemoteToIoFile(project, indicator, driver, rfsPath, tempZipFile)

        val gzipInputStream = GZIPInputStream(tempZipFile.inputStream())
        val logFile = FileUtil.createTempFile("aws-emr-$clusterId", ".log", true)
        gzipInputStream.transferTo(logFile.outputStream())

        val vf = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(logFile.toPath()) ?: throw FileNotFoundException(
          logFile.absolutePath)
        invokeLater {
          FileEditorManager.getInstance(project).openFile(vf, true)
        }
      }
    }
    catch (t: Throwable) {
      NotificationUtils.showExceptionMessage(project, t, HdfsMessagesBundle.message("emr.error"))
    }
  }

  fun createS3LogConnection(project: Project, clusterId: String, s3Url: String?) = actionWrapper {
    val cluster = getClusterInfoModel(clusterId).originObject ?: return@actionWrapper

    val sourceUrl = clearS3Prefixes(cluster.cluster.logUri())
    val relativeUrl = s3Url?.let { clearS3Prefixes(it).withSlash() }
    dependsManager.browseOrOpenConnection(project, cluster, EmrClusterAppInfo(name = "EMR Log", connType = BdtConnectionType.S3,
                                                                              version = "",
                                                                              url = sourceUrl), relativeUrl)
  }

  fun openMasterSftpConnection(project: Project, clusterId: String) = actionWrapper {
    val instanceInfo = getClusterMasterInstance(clusterId) ?: return@actionWrapper
    openSftpConnection(project, clusterId, instanceInfo)
  }

  fun openSftpConnection(project: Project, clusterId: String, instanceInfo: EmrClusterInstanceInfo) {
    val cluster = getClusterDetails(clusterId)
    val url = instanceInfo.publicUrl.ifBlank { null } ?: instanceInfo.privateUrl
    val appInfo = EmrClusterAppInfo(name = url, url = url, connType = BdtConnectionType.SFTP)
    dependsManager.browseOrOpenConnection(project, cluster, appInfo)
  }

  internal fun actionWrapper(body: () -> Unit): Unit = executeOnPooledThread {
    try {
      body()
    }
    catch (t: Throwable) {
      NotificationUtils.showExceptionMessage(project, t, HdfsMessagesBundle.message("emr.error"))
    }
  }

  fun loadClusters(states: List<EmrClusterState>? = listOf(EmrClusterState.RUNNING)): List<ClusterSummary> {
    return client.getClusters(states = states, limit = null)
  }

  fun createSparkConnection(project: Project, clusterDetails: EmrClusterDetails, onInit: (ConnectionData) -> Unit): ConnectionData? {
    val createSparkConnection = createSparkConnection(project, clusterDetails) ?: return null
    driver.fileInfoManager.refreshFiles(RfsPath(listOf(clusterDetails.id), true))
    return createSparkConnection.also(onInit)
  }

  private fun createSparkConnection(project: Project, clusterDetails: EmrClusterDetails): ConnectionData? {
    return dependsManager.getOrCreateDefault(project, clusterDetails, EmrClusterAppUtils.getSparkInfo(clusterDetails))
  }

  companion object {
    private data class StepId(val clusterId: String, val stepId: String)

    fun getInstance(connectionId: String, project: Project): EmrDataManager? =
      (DriverManager.getDriverById(project, connectionId) as? EmrDriver)?.dataManager

    private fun clearS3Prefixes(uri: String) = uri.removePrefix("s3n://").removePrefix("s3://").removePrefix("s3a://")
  }
}