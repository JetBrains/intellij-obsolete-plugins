package com.intellij.bigdatatools.plugin.spark.services

import com.intellij.bigdatatools.coreUi.settings.ConnectionSettingsListener
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.plugin.spark.services.node.BdtBaseNode
import com.intellij.bigdatatools.plugin.spark.services.node.BdtDriverNode
import com.intellij.bigdatatools.plugin.spark.services.node.SparkApplicationInfoNode
import com.intellij.bigdatatools.plugin.spark.services.node.SparkMonitoringDriverNode
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.execution.services.ServiceEventListener
import com.intellij.execution.services.ServiceViewContributor
import com.intellij.execution.services.ServiceViewDescriptor
import com.intellij.execution.services.ServiceViewLazyContributor
import com.intellij.execution.services.ServiceViewManager
import com.intellij.execution.services.ServiceViewToolWindowDescriptor
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.bigdatatools.common.BigdatatoolsCoreIcons
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.concurrency.await
import javax.swing.Icon

class SparkJobServiceViewContributor : ServiceViewContributor<BdtBaseNode>, ServiceViewLazyContributor, Disposable {
  init {
    RfsConnectionDataManager.instance?.addListener(object : ConnectionSettingsListener {
      override fun onConnectionAdded(project: Project?, newConnectionData: ConnectionData) = Utils.refreshAll(project)
      override fun onConnectionRemoved(project: Project?, removedConnectionData: ConnectionData) = Utils.refreshAll(project)
      override fun onConnectionModified(project: Project?, connectionData: ConnectionData, modified: Collection<ModificationKey>) =
        Utils.refreshAll(project)

      override fun getId(): String = LISTENER_ID
    })
  }

  fun focusOnSparkApp(project: Project,
                      connectionData: ConnectionData,
                      info: PresentableApplicationInfo?,
                      ignoreIfError: Boolean) = SafeExecutor.instance.coroutineScope.launch {
    focusOnSparkAppSuspend(project, connectionData, info, ignoreIfError)
  }

  suspend fun focusOnSparkAppSuspend(project: Project,
                                     connectionData: ConnectionData,
                                     info: PresentableApplicationInfo?,
                                     ignoreIfError: Boolean) {
    withContext(Dispatchers.EDT) {
      connectionData as SparkConnectionData

      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.SERVICES)!!
      toolWindow.setAvailable(true)
    }

    val driver = (DriverManager.getDriverById(project, connectionData.innerId) as? SparkMonitoringDriver) ?: return

    driver.waitConnect()
    if (ignoreIfError && driver.dataManager.getConnectionStatus() !is ConnectedConnectionStatus)
      return

    val rootNode = getRootNode(project)

    val rootServices = rootNode.getCachedOrLoadServices(project)
    val bdtDriverNode = rootServices.firstOrNull { (it as? BdtDriverNode)?.connData?.innerId == connectionData.sourceConnection }
                        ?: rootServices.firstOrNull { (it as? BdtDriverNode)?.connData?.innerId == connectionData.innerId }
                        ?: return

    //Spark Monitoring can be inside EMR/Dataproc/etc node or separate
    val targetDriverNode = if (bdtDriverNode is SparkMonitoringDriverNode || connectionData.sourceConnection == null) {
      bdtDriverNode as? SparkMonitoringDriverNode
    }
    else {
      val services = bdtDriverNode.getCachedOrLoadServices(project)
      services.firstOrNull { (it as? BdtDriverNode)?.connData?.innerId == connectionData.innerId } as? SparkMonitoringDriverNode
    }

    targetDriverNode ?: return

    val targetNode = if (info != null) {
      val services = targetDriverNode.getCachedOrLoadServices(project)
      val foundNode = services.firstOrNull { (it as SparkApplicationInfoNode).appInfo.id == info.id } as? SparkApplicationInfoNode
      if (foundNode == null) {
        targetDriverNode.clearCache()
        targetDriverNode.refresh()
        val freshServices = targetDriverNode.getChildren()
        freshServices.firstOrNull { (it as SparkApplicationInfoNode).appInfo.id == info.id } as? SparkApplicationInfoNode
      }
      else {
        foundNode
      }
    }
    else {
      targetDriverNode
    }

    //Wait for load run
    delay(1000)

    val serviceViewManager = ServiceViewManager.getInstance(project)
    withContext(Dispatchers.EDT) {

    serviceViewManager.select(targetNode ?: targetDriverNode, this@SparkJobServiceViewContributor::class.java, true, true).await()
    }

  }

  override fun dispose() {
    RfsConnectionDataManager.instance?.removeListener(LISTENER_ID)
  }

  override fun getViewDescriptor(project: Project): ServiceViewDescriptor =
    object : SimpleServiceViewDescriptor(SMMessagesBundle.message("services.spark.jobs.title"),
                                         BigdatatoolsSparkMonitoringIcons.Spark,
                                         "BDT Spark Jobs"),
             ServiceViewToolWindowDescriptor {
      override fun getToolWindowId(): String = id

      override fun getToolWindowIcon(): Icon = BigdatatoolsCoreIcons.ToolWindowBigData

      override fun getStripeTitle(): String = SMMessagesBundle.message("services.spark.jobs.title")
    }

  override fun getServices(project: Project) = listOf(getRootNode(project))

  override fun getServiceDescriptor(project: Project, service: BdtBaseNode) = service.getNodeViewDescriptor(project)

  private fun getRootNode(project: Project) = SparkJobRootNodeService.Utils.getInstance(project).rootNode


  object Utils {
    fun getInstance(): SparkJobServiceViewContributor? =
      ServiceViewContributor.findRootContributor(SparkJobServiceViewContributor::class.java)

    fun sendEvent(type: ServiceEventListener.EventType,
                  target: Any,
                  rootContributorClass: Class<*>) {
      val event = ServiceEventListener.ServiceEvent.createEvent(type, target, rootContributorClass)
      sendEvent(event)
    }

    private fun sendEvent(event: ServiceEventListener.ServiceEvent) {
      ApplicationManager.getApplication().messageBus.syncPublisher(ServiceEventListener.TOPIC).handle(event)
    }

    fun refreshAll(project: Project?) {
      val projects = project?.let { listOf(it) } ?: ProjectManager.getInstance().openProjects.toList()
      projects.forEach { proj ->
        getInstance()?.getServices(proj)?.forEach {
          it.clearCache()
        }
      }

      sendEvent(ServiceEventListener.ServiceEvent.createResetEvent(SparkJobServiceViewContributor::class.java))
    }
  }

  companion object {
    const val LISTENER_ID = "SparkJobServices"
  }
}
