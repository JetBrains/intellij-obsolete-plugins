package com.jetbrains.bigdatatools.hivemetastore.rfs.metainfo

import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.DriverFileMetaInfoProviderBase
import com.jetbrains.bigdatatools.common.rfs.editorviewer.RfsTableColumn
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveMetastoreDriver

class HiveMetaInfoProvider(override val driver: HiveMetastoreDriver) : DriverFileMetaInfoProviderBase(driver) {
  override fun getDefaultTableColumns(): List<String> = listOf()
  override fun getAllTableColumns(): List<RfsTableColumn<*>> = listOf()

  override fun getFileDetails(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) =
    HiveMetainfoFileDetails(rfsTreeNode, curWindowDisposable)

}