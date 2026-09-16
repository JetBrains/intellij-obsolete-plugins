package com.intellij.bigdatatools.zeppelin.drivers.copyhandler

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.drivers.tasks.ZeppelinToZeppelinCopyTask
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.copyhandler.InterDriverCopyHandler

class ZeppelinToZeppelinCopyHandler : InterDriverCopyHandler {
  override fun canHandle(fromInfo: FileInfo,
                         toDriver: Driver): Boolean = toDriver != fromInfo.driver && fromInfo is ZeppelinFileInfo && toDriver is ZeppelinDriver

  override fun buildCopyTask(fromInfo: FileInfo,
                             toPath: RfsPath,
                             toDriver: Driver,
                             exportFormat: ExportFormat?,
                             additionalParams: Map<String, Any>) = ZeppelinToZeppelinCopyTask(fromInfo, toDriver, toPath)
}

