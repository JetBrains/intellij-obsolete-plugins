package com.intellij.bigdatatools.emr.rfs

import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.DriverFileMetaInfoProviderBase
import com.jetbrains.bigdatatools.common.rfs.editorviewer.RfsTableColumn
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

open class EmrFileMetaInfoProvider(driver: EmrDriver) : DriverFileMetaInfoProviderBase(driver) {
  override fun getAllTableColumns(): List<RfsTableColumn<*>> = emptyList()

  override fun getFileDetails(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) =
    EmrFileDetails(rfsTreeNode, curWindowDisposable)
}