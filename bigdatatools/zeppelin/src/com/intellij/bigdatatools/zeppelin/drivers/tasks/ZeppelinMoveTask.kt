package com.intellij.bigdatatools.zeppelin.drivers.tasks

import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.jetbrains.bigdatatools.common.rfs.copypaste.model.RfsCopyMoveContext
import com.jetbrains.bigdatatools.common.rfs.copypaste.utils.RfsCopyPasteHelpers
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsMoveTask

class ZeppelinMoveTask(private val sourceFileInfo: ZeppelinFileInfo,
                       private val targetPath: RfsPath) : RemoteFsMoveTask(sourceFileInfo, targetPath) {
  override fun run(context: RfsCopyMoveContext) {
    val driver = sourceFileInfo.driver
    val fileSystem = driver.fileSystem

    val notesWithPaths = fileSystem.getSourcesNotesWithTargetPaths(sourceFileInfo, targetPath as ZeppelinRfsPath)
    fileSystem.withCheckConnection {
      notesWithPaths.forEach { sourceAndTarget ->
        val sourcePath = sourceAndTarget.first
        val targetPath = sourceAndTarget.second

        context.startProceedFile(sourcePath.serverPath, targetPath.serverPath, -1)

        if (targetPath.serverPath == sourcePath.serverPath)
          return@withCheckConnection


        val shouldContinue = RfsCopyPasteHelpers.resolveOverwrite(targetPath.serverPath, context) {
          fileSystem.noteExists(targetPath)
        }
        if (!shouldContinue)
          return@withCheckConnection

        fileSystem.getNoteInfoByPath(targetPath)?.let {
          fileSystem.deleteSync(it)
        }

        fileSystem.api.renameNotebookAsync(sourcePath.id, targetPath.serverPath)
      }
    }
    fileSystem.refreshNotes()
  }
}