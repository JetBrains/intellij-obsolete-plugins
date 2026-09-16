package com.intellij.bigdatatools.zeppelin.drivers.copyhandler

import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.drivers.tasks.ZeppelinToLocalCopyTask
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.copyhandler.InterDriverCopyHandler
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalDriver

class ZeppelinToLocalCopyHandler : InterDriverCopyHandler {
  override fun canHandle(fromInfo: FileInfo,
                         toDriver: Driver): Boolean = fromInfo is ZeppelinFileInfo && toDriver is LocalDriver

  override fun correctPathForTarget(fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver, exportFormat: ExportFormat?): RfsPath? {
    if (fromInfo.isDirectory)
      return null
    return toPath.child("${fromInfo.name}.${ZeppelinFileType.defaultExtension}", fromInfo.isDirectory)
  }

  override fun buildCopyTask(fromInfo: FileInfo,
                             toPath: RfsPath,
                             toDriver: Driver,
                             exportFormat: ExportFormat?,
                             additionalParams: Map<String, Any>) =
    ZeppelinToLocalCopyTask(fromInfo, toPath, toDriver, skipIfCopyChildIsNotSupported = true, exportFormat)
}