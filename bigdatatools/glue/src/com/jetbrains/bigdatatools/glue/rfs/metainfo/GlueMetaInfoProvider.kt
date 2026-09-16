package com.jetbrains.bigdatatools.glue.rfs.metainfo

import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.glue.rfs.GlueDriver
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.DriverFileMetaInfoProviderBase
import com.jetbrains.bigdatatools.common.rfs.editorviewer.RfsTableColumn
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

class GlueMetaInfoProvider(override val driver: GlueDriver) : DriverFileMetaInfoProviderBase(driver) {
  override fun getDefaultTableColumns(): List<String> = listOf()
  override fun getAllTableColumns(): List<RfsTableColumn<*>> = listOf()

  override fun getFileDetails(rfsTreeNode: DriverFileRfsTreeNode,
                              curWindowDisposable: Disposable) = GlueFileDetails(rfsTreeNode, curWindowDisposable)
}