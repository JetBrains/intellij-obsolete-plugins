package com.intellij.bigdatatools.zeppelin.drivers.tasks

import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.jetbrains.bigdatatools.common.rfs.copypaste.model.RfsCopyMoveContext
import com.jetbrains.bigdatatools.common.rfs.copypaste.utils.RfsCopyPasteHelpers
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalDriver
import com.jetbrains.bigdatatools.common.rfs.driver.task.BaseRfsCopyMoveTask
import java.io.File

class ZeppelinToLocalCopyTask(fromInfo: FileInfo,
                              toPath: RfsPath,
                              toDriver: Driver,
                              skipIfCopyChildIsNotSupported: Boolean = false,
                              val exportFormat: ExportFormat?) : BaseRfsCopyMoveTask(fromInfo, toPath, toDriver,
                                                                                     skipIfCopyChildIsNotSupported) {
  override fun copyFile(context: RfsCopyMoveContext, fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver) {
    fromInfo as ZeppelinFileInfo
    toDriver as LocalDriver

    context.startProceedFile(fromInfo.path.stringRepresentation(), toPath.stringRepresentation(), -1)

    val targetFile = File(toDriver.canonicalLocalFsPath(toPath))
    if (!RfsCopyPasteHelpers.resolveOverwrite(targetFile.path, context) {
        targetFile.exists()
      }) {
      return
    }

    targetFile.parentFile.mkdirs()
    val json = fromInfo.driver.fileSystem.exportNoteJson(fromInfo.path)
    targetFile.writeText(json)

    toDriver.refreshInProject(toPath)
  }
}