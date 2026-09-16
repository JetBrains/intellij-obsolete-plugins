package com.jetbrains.bigdatatools.dataproc.rfs

import com.google.cloud.dataproc.v1.ClusterStatus
import com.intellij.bigdatatools.hdfs.icons.BigdatatoolsHdfsIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringRfsTreeNode
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverCompoundTreeNode
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.toolwindow.DataprocToolWindowController

internal class DataprocRfsTreeNode(project: Project,
                          rfsPath: RfsPath,
                          private val clusterInfo: DataprocClusterInfo?,
                          driver: DataprocDriver,
                          override val isCompound: Boolean) : MonitoringRfsTreeNode(project, rfsPath, driver), DriverCompoundTreeNode {
  init {
    myName = clusterInfo?.name ?: rfsPath.name
  }

  override fun isAlwaysLeaf() = rfsPath.isFile

  override fun onDoubleClick(): Boolean {
    val project = project ?: return true
    val controller = driver.getController(project) as? DataprocToolWindowController
    controller?.focusOn(focusId, rfsPath.name)
    return true
  }

  override fun getIdleIcon() = when {
    rfsPath.isRoot -> super.getIdleIcon()
    clusterInfo?.state != null -> getIconForCluster(clusterInfo.state)
    else -> null
  }

  override fun update(presentation: PresentationData) {
    super.update(presentation)
    if (clusterInfo != null)
      presentation.presentableText = clusterInfo.name
    @Suppress("HardCodedStringLiteral")
    if (error == null)
      presentation.tooltip = clusterInfo?.state?.name ?: ""
  }


  companion object {
    private val CLUSTER_RUNNING = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Running) }
    private val CLUSTER_ERROR = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Error) }
    private val CLUSTER_EMPTY = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Empty) }
    private val CLUSTER_WARNING = LayeredIcon.layeredIcon { arrayOf(BigdatatoolsHdfsIcons.Cluster, BigdatatoolsHdfsIcons.Warning) }

    internal fun getIconForCluster(clusterInfoState: ClusterStatus.State?) = when (clusterInfoState) {
      ClusterStatus.State.STARTING -> CLUSTER_WARNING
      ClusterStatus.State.RUNNING -> CLUSTER_RUNNING
      ClusterStatus.State.UNKNOWN -> CLUSTER_ERROR
      ClusterStatus.State.CREATING -> CLUSTER_WARNING
      ClusterStatus.State.ERROR -> CLUSTER_ERROR
      ClusterStatus.State.ERROR_DUE_TO_UPDATE -> CLUSTER_ERROR
      ClusterStatus.State.DELETING -> CLUSTER_WARNING
      ClusterStatus.State.UPDATING -> CLUSTER_WARNING
      ClusterStatus.State.STOPPING -> CLUSTER_WARNING
      ClusterStatus.State.STOPPED -> CLUSTER_EMPTY
      ClusterStatus.State.UNRECOGNIZED -> CLUSTER_ERROR
      null -> null
    }
  }
}