package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.plugin.spark.services.node.source.SparkJobServiceAppControllerSource
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.execution.services.ServiceViewLazyContributor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.ui.pages.SparkApplicationController
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class SparkMonitoringDriverNode(project: Project,
                                parent: BdtBaseNode,
                                connectionData: ConnectionData,
                                override val label: String = connectionData.name,
                                override val icon: Icon? = BigdatatoolsSparkMonitoringIcons.Spark) : BdtDriverNode(parent, project,
                                                                                                                   connectionData),
                                                                                                     ServiceViewLazyContributor {
  private val clusterListChangesListener = object : DataModelListener {
    override fun onChangedNonEdt() = refreshChildren()
    override fun onError(msg: String, e: Throwable?) = refreshChildren()
  }

  init {
    (driver?.dataManager as? SparkDataManager)?.applications?.addListener(clusterListChangesListener)
  }

  override fun dispose() {
    super.dispose()
    (driver?.dataManager as? SparkDataManager)?.applications?.removeListener(clusterListChangesListener)
  }

  override fun getChildren(): List<BdtDriverNode> {
    val sparkDataManager = driver?.dataManager as? SparkDataManager ?: return emptyList()
    // If not loaded all try get only not sync
    val applicationInfos = (sparkDataManager.applications.data ?: emptyList()) + sparkDataManager.notSyncAppManager.notSyncApps

    return applicationInfos.map {
      SparkApplicationInfoNode(project, it, this, connData)
    }
  }

  override fun createContentController(): JComponent {
    if (!connData.isEnabled)
      return createConnectionDisabledPresentation()

    val driver = driver as? SparkMonitoringDriver
    val dataManager = driver?.dataManager
    dataManager ?: return createErrorPresentation(MessagesBundle.message("services.panel.connection.error.unexpected"))

    val controller = SparkApplicationController(project, dataManager, SparkJobServiceAppControllerSource(driver))
    if (!isDisposed.get()) {
      Disposer.register(this, controller)
      return controller.getComponent()
    }
    else {
      Disposer.dispose(this)
      return JPanel()
    }

  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SparkMonitoringDriverNode

    return connData == other.connData
  }

  override fun hashCode(): Int {
    return connData.hashCode()
  }
}