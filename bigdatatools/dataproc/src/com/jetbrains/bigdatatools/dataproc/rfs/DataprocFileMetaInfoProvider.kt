package com.jetbrains.bigdatatools.dataproc.rfs

import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.DriverFileMetaInfoProviderBase
import com.jetbrains.bigdatatools.common.rfs.editorviewer.RfsTableColumn
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

open class DataprocFileMetaInfoProvider(driver: DataprocDriver) : DriverFileMetaInfoProviderBase(driver) {
  override fun getAllTableColumns(): List<RfsTableColumn<*>> = emptyList()

  override fun getFileDetails(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) =
    DataprocFileDetails(rfsTreeNode, curWindowDisposable)
}