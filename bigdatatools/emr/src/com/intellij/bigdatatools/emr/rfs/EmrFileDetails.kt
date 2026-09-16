package com.intellij.bigdatatools.emr.rfs

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoBlock
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoDetailsBase
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.rowIfNotBlank
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

class EmrFileDetails(rfsTreeNode: DriverFileRfsTreeNode,
                     parentDisposable: Disposable) : FileInfoDetailsBase(rfsTreeNode,
                                                                         parentDisposable) {
  override fun getBlocks(): List<FileInfoBlock> {
    val panel = MigPanel(UiUtil.insets10FillXHidemode3).apply {
      showErrorInfo(rfsTreeNode)

      val driver = rfsTreeNode.driver as EmrDriver
      val rfsPath = rfsTreeNode.rfsPath
      val cluster = driver.dataManager.getClusterById(rfsPath.name)
      val clusterSummary = cluster?.origin ?: return@apply

      rowIfNotBlank(EmrMessagesBundle.message("metainfo.cluster.name"), clusterSummary.name())
      rowIfNotBlank(EmrMessagesBundle.message("metainfo.cluster.id"), clusterSummary.id())
      rowIfNotBlank(EmrMessagesBundle.message("metainfo.cluster.status"), clusterSummary.status()?.stateAsString())
      rowIfNotBlank(EmrMessagesBundle.message("metainfo.cluster.normalizedInstanceHours"),
                    clusterSummary.normalizedInstanceHours()?.toString())
      rowIfNotBlank(EmrMessagesBundle.message("metainfo.cluster.arn"), clusterSummary.clusterArn())
      rowIfNotBlank(EmrMessagesBundle.message("metainfo.cluster.outpostArn"), clusterSummary.outpostArn())
    }
    return listOf(FileInfoBlock("", panel))
  }
}