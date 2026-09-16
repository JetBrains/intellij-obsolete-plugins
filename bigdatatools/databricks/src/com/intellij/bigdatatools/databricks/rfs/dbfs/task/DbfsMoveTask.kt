package com.intellij.bigdatatools.databricks.rfs.dbfs.task

import com.intellij.bigdatatools.databricks.rfs.dbfs.DbfsFileInfo
import com.intellij.bigdatatools.coreUi.util.prefixIfNot
import com.jetbrains.bigdatatools.common.rfs.copypaste.model.RfsCopyMoveContext
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsMoveTask

internal class DbfsMoveTask(private val sourceFileInfo: DbfsFileInfo,
                   private val newPath: RfsPath) : RemoteFsMoveTask(sourceFileInfo, newPath) {
  override fun isNeedPrecalculate() = false

  override fun run(context: RfsCopyMoveContext) {
    val sourcePath = sourceFileInfo.path.stringRepresentation().prefixIfNot("/")
    val destPath = newPath.stringRepresentation().prefixIfNot("/")
    context.startProceedFile(sourcePath, destPath, sourceFileInfo.length)
    sourceFileInfo.client.dbfsMove(sourcePath, destPath)

  }
}