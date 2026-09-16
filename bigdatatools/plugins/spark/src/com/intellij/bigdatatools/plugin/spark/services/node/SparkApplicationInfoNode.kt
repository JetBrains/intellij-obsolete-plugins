package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.plugin.spark.services.node.source.SparkJobServiceAppControllerSource
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.monitoring.ApplicationsStartedFromIdeRegistry
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.ui.pages.SparkAppInfoController
import com.jetbrains.spark.monitoring.ui.utils.IconUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

class SparkApplicationInfoNode(project: Project,
                               val appInfo: PresentableApplicationInfo,
                               parent: BdtBaseNode,
                               connectionData: ConnectionData) : BdtDriverNode(parent, project,
                                                                               connectionData) {
  override val label: String = appInfo.id.toString()
  override val icon: Icon? = IconUtils.getIconForApplicationStatus(appInfo.status)

  override val greyText: String = let {
    val isOurTask = appInfo.customId != null ||
                    serviceOrNull<ApplicationsStartedFromIdeRegistry>()?.getSourceConfigurationId(project, appInfo.appId,
                                                                                                  connectionData) != null
    val isOurTaskLabel = if (isOurTask)
      SparkMessagesBundle.message("app.by.me.value")
    else
      ""
    (appInfo.duration?.let { TimeUtils.intervalAsString(it) + " " } ?: "") + isOurTaskLabel
  }

  override fun getViewDescriptor(project: Project) = SimpleServiceViewDescriptor(label, icon)
  override fun getAllowsChildren(): Boolean = false
  override fun isLeaf(): Boolean = true
  override fun getChildren(): List<BdtDriverNode> = emptyList()


  private val controller: SparkAppInfoController? by lazy {
    val sparkDriver = driver as SparkMonitoringDriver
    val controller = SparkAppInfoController(project, sparkDriver.dataManager, SparkJobServiceAppControllerSource(sparkDriver))
    if (isDisposed.get())
      return@lazy null
    Disposer.register(this, controller)
    controller.setDetailsId(appInfo.id)
    controller
  }

  override fun createContentController() = controller?.getComponent() ?: createErrorPresentation(
    MessagesBundle.message("services.panel.connection.error.unexpected"))

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SparkApplicationInfoNode


    if (connData.innerId != other.connData.innerId)
      return false
    if (appInfo.customId != null && appInfo.customId == other.appInfo.customId)
      return true
    if (appInfo.id != other.appInfo.id)
      return false
    return true
  }

  override fun hashCode(): Int {
    return appInfo.id.hashCode() + connData.innerId.hashCode()
  }
}