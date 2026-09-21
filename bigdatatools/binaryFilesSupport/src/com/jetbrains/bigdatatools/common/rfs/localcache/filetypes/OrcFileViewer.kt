package com.jetbrains.bigdatatools.common.rfs.localcache.filetypes

import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.localcache.MagicAware
import com.jetbrains.bigdatatools.common.rfs.view.GeneralContentTypeViewerBase

class OrcFileViewer : GeneralContentTypeViewerBase(), MagicAware by ORCContentDownloader.Util {
  override fun accept(driver: Driver, rfsPath: RfsPath): Boolean = ORCContentDownloader.Util.accept(rfsPath)
  override fun getFileName(fileInfo: FileInfo) = fileInfo.name.replaceAfterLast('.', "orc")
}