package com.intellij.bigdatatools.emr.rfs

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.rfs.EmrDriver.Companion.isCluster
import com.intellij.bigdatatools.emr.toolwindow.EmrToolWindowController
import com.intellij.bigdatatools.hdfs.icons.BigdatatoolsHdfsIcons
import com.intellij.bigdatatools.sftp.icons.BigdatatoolsSftpIcons
import com.intellij.bigdatatools.sparkSubmit.icons.BigdatatoolsSparkSubmitIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringRfsTreeNode
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverCompoundTreeNode
import software.amazon.awssdk.services.emr.model.ClusterState
import javax.swing.Icon

class EmrRfsTreeNode(project: Project,
                     rfsPath: RfsPath,
                     private val clusterInfo: EmrClusterInfo?,
                     driver: MonitoringDriver,
                     override val isCompound: Boolean) : MonitoringRfsTreeNode(project, rfsPath, driver), DriverCompoundTreeNode {
  init {
    myName = if (rfsPath.parent?.isCluster == true)
      rfsPath.name
    else
      (clusterInfo?.name ?: rfsPath.name)
  }

  override fun isAlwaysLeaf() = rfsPath.isFile || clusterInfo?.isStopped == true

  override fun onDoubleClick(): Boolean {
    val project = project ?: return true
    if (rfsPath.parent?.isCluster == true) {
      val connectionType = BdtConnectionType.entries.firstOrNull { it.connName == rfsPath.name } ?: return true
      clusterInfo ?: return true
      (driver.dataManager as EmrDataManager).dependsManager.createConnection(project, clusterInfo, connectionType)
      return true
    }
    val controller = driver.getController(project) as? EmrToolWindowController
    controller?.focusOn(focusId, rfsPath.name)
    return true
  }

  override fun getIdleIcon() = when {
    rfsPath.isRoot -> super.getIdleIcon()
    rfsPath.parent?.isCluster == true -> {

      val connectionType = BdtConnectionType.entries.firstOrNull { it.connName == rfsPath.name }
      when (connectionType) {
        BdtConnectionType.SPARK_MONITORING -> BigdatatoolsSparkSubmitIcons.Spark
        BdtConnectionType.SFTP -> BigdatatoolsSftpIcons.Sftp
        else -> null
      }
    }
    clusterInfo?.state != null -> getIconForCluster(clusterInfo.state)
    else -> null
  }

  override fun update(presentation: PresentationData) {
    super.update(presentation)
    when {
      rfsPath.isRoot -> presentation.presentableText = driver.presentableName
      rfsPath.parent?.isCluster == true -> {
        presentation.addText(rfsPath.name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
        presentation.tooltip = MessagesBundle.message("rfs.action.create.depend.title", rfsPath.name)
      }
      else -> {
        @Suppress("HardCodedStringLiteral")
        presentation.tooltip = clusterInfo?.state?.name ?: ""
        presentation.presentableText = (clusterInfo?.name ?: rfsPath.name)
      }
    }
  }

  companion object {
    private val CLUSTER_RUNNING = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Running) }
    private val CLUSTER_ERROR = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Error) }
    private val CLUSTER_EMPTY = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Empty) }
    private val CLUSTER_WARNING = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Warning) }

    fun getIconForCluster(clusterInfo: EmrClusterInfo): Icon? = getIconForCluster(clusterInfo.state)

    internal fun getIconForCluster(clusterInfoState: ClusterState?): Icon? = when (clusterInfoState) {
      ClusterState.STARTING -> CLUSTER_WARNING
      ClusterState.BOOTSTRAPPING -> CLUSTER_WARNING
      ClusterState.RUNNING -> CLUSTER_RUNNING
      ClusterState.WAITING -> CLUSTER_RUNNING
      ClusterState.TERMINATING -> CLUSTER_EMPTY
      ClusterState.TERMINATED -> CLUSTER_EMPTY
      ClusterState.TERMINATED_WITH_ERRORS -> CLUSTER_ERROR
      ClusterState.UNKNOWN_TO_SDK_VERSION -> CLUSTER_ERROR
      null -> null
    }
  }
}