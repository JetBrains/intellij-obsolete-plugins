package com.intellij.bigdatatools.zeppelin.drivers.metainfo

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.SelectableLabel
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoBlock
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoDetailsBase
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

class ZeppelinFileDetails(rfsTreeNode: DriverFileRfsTreeNode, parentDisposable: Disposable) : FileInfoDetailsBase(rfsTreeNode,
                                                                                                                  parentDisposable) {
  override fun getBlocks(): List<FileInfoBlock> {
    val panel = MigPanel(UiUtil.insets10FillXHidemode3).apply {
      val path = rfsTreeNode.rfsPath
      row(MessagesBundle.message("file.info.label.path"), SelectableLabel(path.stringRepresentation()))
      showErrorInfo(rfsTreeNode)
    }
    return listOf(FileInfoBlock("", panel))
  }
}