package com.intellij.bigdatatools.zeppelin.drivers.copyhandler

import com.intellij.bigdatatools.zeppelin.inote.file.INoteFileType
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.copyhandler.InterDriverCopyHandler
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.task.ImportCopyTask

class LocalToZeppelinCopyHandler : InterDriverCopyHandler {
  private val supportedFormats = setOf("json",
                                       ZeppelinFileType.defaultExtension,
                                       INoteFileType.defaultExtension)


  override fun canHandle(fromInfo: FileInfo, toDriver: Driver): Boolean =
    fromInfo is LocalFileInfo && toDriver is ZeppelinDriver &&
    (fromInfo.isFile || fromInfo.isDirectory)

  override fun correctPathForTarget(fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver, exportFormat: ExportFormat?) =
    if (fromInfo.isDirectory)
      null
    else {
      val clearName = fromInfo.name.dropLastWhile { it != '.' }.dropLast(1)
      toPath.child(clearName, fromInfo.isDirectory)
    }


  override fun buildCopyTask(fromInfo: FileInfo,
                             toPath: RfsPath,
                             toDriver: Driver,
                             exportFormat: ExportFormat?,
                             additionalParams: Map<String, Any>): ImportCopyTask {
    if (fromInfo.isFile && fromInfo.name.takeLastWhile { it != '.' } !in supportedFormats)
      throw Exception(ZepMessagesBundle.message("import.error.supported.type", supportedFormats.joinToString()))
    return ImportCopyTask(fromInfo, toPath, toDriver, skipIfCopyChildIsNotSupported = true,
                          exportFormat)
  }
}