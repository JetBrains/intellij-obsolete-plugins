package com.jetbrains.bigdatatools.dataproc.data

import com.google.cloud.dataproc.v1.Job
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SystemNotifications
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.updater.WaitConditionTask
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.dataproc.client.BdtDataprocClient
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDependsManager
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterState
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo.Companion.jobId
import com.jetbrains.bigdatatools.dataproc.model.DataprocVmInstanceInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo
import com.jetbrains.bigdatatools.dataproc.model.VmInstanceRole
import com.jetbrains.bigdatatools.dataproc.rfs.DataprocDriver
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionData
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.util.DataprocDriverCreator
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle

class DataprocDataManager(project: Project?,
                          override val connectionData: DataprocConnectionData,
                          settings: DataprocToolWindowSettings) : MonitoringDataManager(project, settings) {

  val driverCreator = DataprocDriverCreator(this)

  val region = connectionData.region

  override val client = BdtDataprocClient(project, connectionData)

  val allClusterModel = createAllClusterDataModel().also { Disposer.register(this, it) }
  val clusterModel = createClusterDataModel().also { Disposer.register(this, it) }
  private var clusterInfoModels = createClusterInfos().also { Disposer.register(this, it) }
  private var clusterJobsModels = createJobs(settings).also { Disposer.register(this, it) }
  private var clusterVmInstancesModels = createVmInstances().also { Disposer.register(this, it) }
  private var clusterWebInterfacesModels = createClusterWebInterfaces().also { Disposer.register(this, it) }
  private var clusterJobInfoModels = createClusterJobs().also { Disposer.register(this, it) }


  val dependsManager = DataprocDependsManager(this).also {
    Disposer.register(this, it)
  }

  init {
    allClusterModel.addListener(object : DataModelListener {
      override fun onChanged() {
        val clusters = allClusterModel.data ?: emptyList()
        if (clusters.isEmpty())
          return
        dependsManager.updateDependsByClusterLister(clusters)
      }
    })
    init()
    RootDataModelStorage(updater, listOf(allClusterModel, clusterModel)).also { Disposer.register(this, it) }
  }

  fun getClusterById(id: String) = clusterModel.entries.firstOrNull { it.id == id }
  fun getClusterByName(name: String) = clusterModel.entries.firstOrNull { it.name == name }
  fun loadCluster(name: String) = client.getCluster(name)

  fun getClusterVmInstancesDataModel(clusterName: String?) = clusterVmInstancesModels[clusterName]
  fun geWebInterfacesDataModel(clusterName: String?) = clusterWebInterfacesModels[clusterName]
  fun getClusterJobsDataModel(clusterName: String?) = clusterJobsModels[clusterName]
  fun getJobInfoModel(clusterId: String, jobId: String) = clusterJobInfoModels[ClusterJobId(clusterId, jobId)]


  fun getClusterInfoModel(clusterName: String): FieldsGroupModel<DataprocClusterInfo> = clusterInfoModels[clusterName]

  fun isClusterRun(clusterName: String?) = clusterName?.let { getClusterByName(it)?.isStopped } == false

  fun createSparkConnection(project: Project, cluster: DataprocClusterInfo, onInit: (ConnectionData) -> Unit = {}): ConnectionData? {
    val appInfo = getApplications(cluster).firstOrNull {
      it.connType == BdtConnectionType.SPARK_MONITORING
    } ?: return null
    val connectionData = dependsManager.getOrCreateDefault(project, cluster, appInfo) ?: return null
    onInit(connectionData)
    return connectionData
  }
  private fun createAllClusterDataModel(): ObjectDataModel<DataprocClusterInfo> {
    val model = ObjectDataModel(DataprocClusterInfo::id) {
      client.getClusters().map { DataprocClusterInfo.getFrom(it) } to false
    }

    Disposer.register(this, model)
    return model
  }

  private fun createClusterDataModel(): ObjectDataModel<DataprocClusterInfo> {
    val model = ObjectDataModel(DataprocClusterInfo::id, allowAutoRefresh = false) {
      allClusterModel.error?.let { throw it.cause ?: it }
      val dataprocClusterInfos = allClusterModel.data ?: emptyList()

      val settings = DataprocToolWindowSettings.getInstance()
      val config = settings.getOrCreateConfig(connectionData.innerId)


      val textFilter = config.textFilter ?: ""
      val limit = config.clusterLimit
      val supportedStates = settings.customClusterStates

      dataprocClusterInfos.filter { info ->
        val filterText = textFilter.isBlank() ||
                         info.cluster.labelsMap.entries.joinToString { "${it.key}=${it.value}" }.contains(textFilter) ||
                         info.cluster.clusterName.contains(textFilter)
        val filterState = supportedStates.isEmpty() || supportedStates.any { it.isSupported(info.cluster) }
        filterText && filterState
      }.take(limit ?: 5000) to false
    }

    model.subscribeOn(allClusterModel)
    Disposer.register(this, model)
    return model
  }

  fun removeCluster(clusterName: String) = actionWrapper(null) {
    client.removeCluster(clusterName)
    this.updater.invokeRefreshModel(clusterModel)
  }


  fun startCluster(clusterName: String) = actionWrapper(null) {
    client.startCluster(clusterName)
    this.updater.invokeRefreshModel(clusterModel)
    this.updater.invokeRefreshModel(getClusterInfoModel(clusterName))
  }


  fun terminateCluster(clusterName: String) = actionWrapper(null) {
    client.terminateCluster(clusterName)
    this.updater.invokeRefreshModel(clusterModel)
    this.updater.invokeRefreshModel(getClusterInfoModel(clusterName))
  }

  fun addJob(job: Job) = actionWrapper(job.placement?.clusterName) {
    val jobId = client.addJob(job)

    val clusterName: String? = job.placement.clusterName
    this.updater.invokeRefreshModel(getClusterJobsDataModel(null))
    clusterName?.let { this.updater.invokeRefreshModel(getClusterJobsDataModel(it)) }

    jobId?.let { addJobNotification(it, clusterName) }
  }

  private fun addJobNotification(stepId: String, clusterName: String?) {
    WaitConditionTask.start(this, checkIsFinished = {
      if (client.isConnected())
        client.checkIsJobFinished(stepId)
      else
        false
    }, onFinish = {
      this.updater.invokeRefreshModel(getClusterJobsDataModel(null))
      clusterName?.let { this.updater.invokeRefreshModel(getClusterJobsDataModel(it)) }

      val step = client.getJobInfo(stepId)

      @Suppress("DialogTitleCapitalization")
      val title = DataprocMessagesBundle.message("cell.execution.finished.title")
      val msg = DataprocMessagesBundle.message("cell.execution.finished.msg", stepId,
                                               step?.status?.state?.name ?: "<Unknown>")

      SystemNotifications.getInstance().notify(NotificationGroupManager.getInstance().getNotificationGroup("Job Notification").displayId,
                                               title, msg)
      NotificationUtils.notifySuccess(msg, title, project = project)
    })

  }

  fun cancelJob(job: Job) = actionWrapper(job.placement?.clusterName) {
    client.cancelJob(job.jobId)

    val clusterName: String = job.placement.clusterName
    this.updater.invokeRefreshModel(getClusterJobsDataModel(null))
    this.updater.invokeRefreshModel(getClusterJobsDataModel(clusterName))
  }

  fun deleteJob(job: Job) = try {
    client.deleteJob(job.jobId)

    val clusterName: String = job.placement.clusterName
    this.updater.invokeRefreshModel(getClusterJobsDataModel(null))
    this.updater.invokeRefreshModel(getClusterJobsDataModel(clusterName))
  }
  catch (t: Throwable) {
    NotificationUtils.showExceptionMessage(project, t, DataprocMessagesBundle.message("dataproc.error"))
  }


  private fun createVmInstances() = ObjectDataModelStorage<String?, DataprocVmInstanceInfo>(updater,
                                                                                            DataprocVmInstanceInfo::name,
                                                                                            dependOn = clusterModel) { clusterName ->
    val clusters = clusterModel.data ?: emptyList()
    val cluster = clusters.firstOrNull { it.name == clusterName } ?: error("Cluster $clusterName is not found")
    val masterInstances = cluster.cluster.config?.masterConfig?.let {
      DataprocVmInstanceInfo.createFrom(it, VmInstanceRole.MASTER, cluster)
    } ?: emptyList()
    val workerInstances = cluster.cluster.config?.workerConfig?.let {
      DataprocVmInstanceInfo.createFrom(it, VmInstanceRole.WORKER, cluster)
    } ?: emptyList()
    masterInstances + workerInstances
  }


  private fun createClusterInfos() = FieldGroupsDataModelStorage<String, DataprocClusterInfo>(updater,
                                                                                              dependOn = clusterModel) { clusterName ->
    @Suppress("UNREACHABLE_CODE")
    val clusterInfo = clusterModel.data?.firstOrNull { it.name == clusterName } ?: return@FieldGroupsDataModelStorage error("Not Found")

    DataprocDataManagerUtils.getClusterInfo(clusterInfo)
  }

  private fun createJobs(settings: DataprocToolWindowSettings) =
    ObjectDataModelStorage<String?, DataprocJobInfo>(updater, DataprocJobInfo::id) { clusterName ->
      val config = settings.getOrCreateConfig(connectionData.innerId)

      val newValue = client.getJobs(
        clusterName = clusterName,
        supportedStates = settings.customJobStates,
        textFilter = config.jobFilter ?: "",
        limit = config.jobLimit)

      newValue.map { DataprocJobInfo(it) }
    }

  private fun createClusterWebInterfaces() = ObjectDataModelStorage<String?, DataprocWebInterfaceInfo>(updater,
                                                                                                       DataprocWebInterfaceInfo::name,
                                                                                                       dependOn = clusterModel) { clusterName ->
    val clusters = clusterModel.data ?: emptyList()
    val cluster = clusters.firstOrNull { it.name == clusterName } ?: return@ObjectDataModelStorage emptyList()
    getApplications(cluster)
  }

  private fun createClusterJobs() = FieldGroupsDataModelStorage<ClusterJobId, DataprocJobInfo>(updater) { clusterJobId ->
    val clusterId = clusterJobId.clusterId
    val jobId = clusterJobId.jobId
    val sourceModel = getClusterJobsDataModel(clusterId)
    val source = sourceModel.data ?: emptyList()
    val clusterJobInfo = source.first { it.id == jobId }
    DataprocDataManagerUtils.createJobInfo(clusterJobInfo)
  }

  internal fun actionWrapper(checkClusterName: String?, body: () -> Unit) = executeOnPooledThread {
    val errorTitle = DataprocMessagesBundle.message("dataproc.error")
    try {
      if (checkClusterName != null) {
        if (!isClusterRun(checkClusterName)) {
          val errorMsg = DataprocMessagesBundle.message("dataproc.error.cluster.must.be.started")
          NotificationUtils.showInfoMessage(project, errorMsg, errorTitle)
          return@executeOnPooledThread
        }
      }
      body()
    }
    catch (t: Throwable) {
      NotificationUtils.showExceptionMessage(project, t, errorTitle)
    }
  }

  fun loadClusters(supportedStates: List<DataprocClusterState>): List<DataprocClusterInfo> {
    return client.getClusters(supportedStates = supportedStates).map { DataprocClusterInfo(it) }
  }

  companion object {
    data class ClusterJobId(val clusterId: String, val jobId: String)


    fun getInstance(connectionId: String, project: Project): DataprocDataManager? =
      (DriverManager.getDriverById(project, connectionId) as? DataprocDriver)?.dataManager

    fun getApplications(cluster: DataprocClusterInfo): List<DataprocWebInterfaceInfo> {
      val config = cluster.cluster.config
      val endpointConfig = config.endpointConfig
      return if (endpointConfig?.enableHttpPortAccess == true) {
        val portsMap = endpointConfig.httpPortsMap ?: emptyMap()
        portsMap.entries.map { DataprocWebInterfaceInfo(name = it.key, it.value, componentGateway = true) }
      }
      else {
        val original = listOf(
          DataprocWebInterfaceInfo(name = "YARN ResourceManager", "http://localhost:8088", componentGateway = false),
          DataprocWebInterfaceInfo(name = "HDFS NameNode", "http://localhost:9870", componentGateway = false),
          DataprocWebInterfaceInfo(name = "Spark History Server", "http://localhost:18080", componentGateway = false),
        )
        val additional = config.softwareConfig?.optionalComponentsList?.mapNotNull {
          when (it.name) {
            "JUPYTER" -> DataprocWebInterfaceInfo(name = "Jupyter", "http://localhost:8123", componentGateway = false)
            "ZEPPELIN" -> DataprocWebInterfaceInfo(name = "Zeppelin", "http://localhost:8080", componentGateway = false)
            "PRESTO" -> DataprocWebInterfaceInfo(name = "Presto", "http://localhost:8060", componentGateway = false)
            else -> null
          }
        } ?: emptyList()

        original + additional
      }
    }
  }
}