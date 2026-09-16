package com.jetbrains.bigdatatools.dataproc.rfs

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoBlock
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoDetailsBase
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.rowIfNotBlank
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManagerUtils
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle

class DataprocFileDetails(rfsTreeNode: DriverFileRfsTreeNode, parentDisposable: Disposable) : FileInfoDetailsBase(rfsTreeNode,
                                                                                                                  parentDisposable) {
  override fun getBlocks(): List<FileInfoBlock> {
    val panel = MigPanel(UiUtil.insets10FillXHidemode3).apply {
      showErrorInfo(rfsTreeNode)

      val clusterSummary = (rfsTreeNode.fileInfo as? DataprocFileInfo)?.clusterInfo ?: return@apply

      rowIfNotBlank(DataprocMessagesBundle.message("metainfo.cluster.name"), clusterSummary.name)
      rowIfNotBlank(DataprocMessagesBundle.message("metainfo.cluster.id"), clusterSummary.id)
      rowIfNotBlank(DataprocMessagesBundle.message("metainfo.cluster.status"), clusterSummary.status)

      DataprocDataManagerUtils.getClusterDescriptionFields(clusterSummary).filter { (it.second?.toString() ?: "").isNotBlank() }.forEach {
        @Suppress("HardCodedStringLiteral")
        rowIfNotBlank(it.first, it.second?.toString() ?: "")
      }
    }
    return listOf(FileInfoBlock("", panel))
  }
}