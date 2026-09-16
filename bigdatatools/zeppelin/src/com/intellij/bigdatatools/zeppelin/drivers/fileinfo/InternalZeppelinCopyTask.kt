package com.intellij.bigdatatools.zeppelin.drivers.fileinfo

import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.jetbrains.bigdatatools.common.rfs.copypaste.model.RfsCopyMoveContext
import com.jetbrains.bigdatatools.common.rfs.copypaste.utils.RfsCopyPasteHelpers
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.BaseRfsCopyMoveTask


class InternalZeppelinCopyTask(sourceFileInfo: ZeppelinFileInfo,
                               toPath: RfsPath) : BaseRfsCopyMoveTask(sourceFileInfo, toPath, sourceFileInfo.driver) {
  private val zeppelinFileSystem = sourceFileInfo.driver.fileSystem

  override fun run(context: RfsCopyMoveContext) = try {
    super.run(context)
  }
  finally {
    zeppelinFileSystem.refreshNotes()
  }

  override fun copyFile(context: RfsCopyMoveContext, fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver) {
    val sourcePath = fromInfo.path as ZeppelinRfsPath
    val targetPath = toPath as ZeppelinRfsPath
    context.startProceedFile(sourcePath.serverPath, targetPath.serverPath, -1)

    val shouldContinue = RfsCopyPasteHelpers.resolveOverwrite(targetPath.serverPath, context) {
      zeppelinFileSystem.noteExists(targetPath)
    }
    if (!shouldContinue)
      return

    zeppelinFileSystem.withCheckConnection {
      val pathWithId = zeppelinFileSystem.getNoteInfoByPath(targetPath)
      pathWithId?.let {
        zeppelinFileSystem.deleteSync(it)
      }

      zeppelinFileSystem.cloneNote(sourcePath, toPath)
    }
  }
}