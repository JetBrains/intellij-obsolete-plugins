package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.sftp.icons.BigdatatoolsSftpIcons
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringRfsTreeNode
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverCompoundTreeNode

internal class ArbitraryClusterRfsTreeNode(project: Project,
                                           rfsPath: RfsPath,
                                           driver: ArbitraryClusterDriver,
                                           override val isCompound: Boolean) : MonitoringRfsTreeNode(project, rfsPath,
                                                                                                     driver), DriverCompoundTreeNode {
  init {
    myName = rfsPath.name
  }

  override fun isAlwaysLeaf() = rfsPath.isFile

  override fun onDoubleClick(): Boolean {
    val project = project ?: return true
    if (rfsPath.isRoot) {
      MonitoringServiceProvider.getSparkMonitoringService()?.focusOn(project, driver.connectionData, rfsPath.name, false)
    }
    else {
      val connectionType = BdtConnectionType.entries.firstOrNull { it.connName == rfsPath.name } ?: return true
      (driver.dataManager as ArbitraryClusterDataManager).dependsManager.createConnection(project, connectionType)
    }
    return true
  }

  override fun getIdleIcon() = when {
    rfsPath.isRoot -> super.getIdleIcon()
    rfsPath.name == BdtConnectionType.SFTP.connName -> BigdatatoolsSftpIcons.Sftp
    rfsPath.name == BdtConnectionType.SPARK_MONITORING.connName -> BigdatatoolsSparkMonitoringIcons.Spark
    else -> null
  }

  override fun update(presentation: PresentationData) {
    super.update(presentation)
    if (rfsPath.parent?.isRoot == true) {
      presentation.addText(rfsPath.name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
      presentation.tooltip = MessagesBundle.message("rfs.action.create.depend.title", rfsPath.name)
    }
  }
}