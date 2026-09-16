package com.intellij.bigdatatools.zeppelin.drivers.tasks

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.rfs.path.zeppelinAbsolutePath
import com.jetbrains.bigdatatools.common.rfs.copypaste.model.RfsCopyMoveContext
import com.jetbrains.bigdatatools.common.rfs.copypaste.utils.RfsCopyPasteHelpers
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.BaseRfsCopyMoveTask

class ZeppelinToZeppelinCopyTask(fromInfo: FileInfo,
                                 toDriver: Driver,
                                 toPath: RfsPath) : BaseRfsCopyMoveTask(fromInfo, toPath, toDriver) {
  override fun copyFile(context: RfsCopyMoveContext, fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver) {
    fromInfo as ZeppelinFileInfo
    toDriver as ZeppelinDriver
    val fromDriver = fromDriver as ZeppelinDriver

    context.startProceedFile(fromInfo.path.stringRepresentation(), toPath.stringRepresentation(), -1)

    if (!RfsCopyPasteHelpers.resolveOverwriteFileInfo(toDriver, toPath, context))
      return

    val exported = fromDriver.exportNote(fromInfo.path)?.note ?: return
    exported.performModification {
      exported.name = toPath.zeppelinAbsolutePath().removePrefix("/")
    }
    toDriver.importNote(exported)
  }
}