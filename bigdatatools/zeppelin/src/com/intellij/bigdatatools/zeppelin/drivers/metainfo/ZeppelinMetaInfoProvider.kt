package com.intellij.bigdatatools.zeppelin.drivers.metainfo

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.DriverFileMetaInfoProviderBase
import com.jetbrains.bigdatatools.common.rfs.editorviewer.RfsTableColumn
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

class ZeppelinMetaInfoProvider(zeppelinDriver: ZeppelinDriver) : DriverFileMetaInfoProviderBase(zeppelinDriver) {
  override fun getDefaultComparator(): Comparator<FileInfo> = compareBy(
    { (it as ZeppelinFileInfo).isTrashRoot },
    { !it.isDirectory }, { it.name }
  )

  override fun getDefaultTableColumns(): List<String> = listOf()
  override fun getAllTableColumns(): List<RfsTableColumn<*>> = listOf()

  override fun getFileDetails(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) =
    ZeppelinFileDetails(rfsTreeNode, curWindowDisposable)
}