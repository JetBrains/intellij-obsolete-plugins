package com.intellij.bigdatatools.databricks.rfs.copyhandler

import com.intellij.bigdatatools.databricks.rfs.dbfs.DbfsDriver
import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils
import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils.originInfo
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceDriver
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.copyhandler.InterDriverCopyHandler
import com.jetbrains.bigdatatools.common.rfs.driver.task.ImportCopyTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.ReadStreamToWriteStreamFsCopyTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask

class DatabricksToDatabricksCopyHandler : InterDriverCopyHandler {
  override fun canHandle(fromInfo: FileInfo,
                         toDriver: Driver): Boolean = DbRfsUtils.isCorrectFileInfo(fromInfo) &&
                                                      DbRfsUtils.isCorrectDriver(toDriver)

  override fun correctPathForTarget(fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver, exportFormat: ExportFormat?): RfsPath? =
    DbRfsUtils.correctPathForWorkspaceTarget(fromInfo, toPath, toDriver)


  override fun buildCopyTask(fromInfo: FileInfo,
                             toPath: RfsPath,
                             toDriver: Driver,
                             exportFormat: ExportFormat?,
                             additionalParams: Map<String, Any>): RfsCopyMoveTask {
    val sourceFileInfo = fromInfo.originInfo

    val targetDriver = when {
      //toDriver is DatabricksDriver && toPath.startsWith(DBFS_ROOT_PATH) -> toDriver.dbfsDriver
      //toDriver is DatabricksDriver && toPath.startsWith(WORKSPACE_ROOT_PATH) -> toDriver.workspaceDriver
      toDriver is DbfsDriver -> toDriver
      toDriver is DatabricksWorkspaceDriver -> toDriver
      else -> error("Wrong Driver class ${toDriver::class.qualifiedName}")
    }

    val realToPath = when (toDriver) {
      is DbfsDriver -> toPath
      is DatabricksWorkspaceDriver -> toPath
      else -> toPath.dropPrefix(1)
    }

    return if (targetDriver is DatabricksWorkspaceDriver) {
      val realExportFormat = if (sourceFileInfo is DatabricksWorkspaceFileInfo)
        DatabricksWorkspaceFileInfo.DBC_EXPORT_FORMAT
      else
        DatabricksWorkspaceFileInfo.exportFormatByExtension(fromInfo.name.split(".").last())

      ImportCopyTask(fromInfo = sourceFileInfo,
                     toPath = realToPath,
                     toDriver = targetDriver,
                     skipIfCopyChildIsNotSupported = true,
                     exportFormat = realExportFormat)
    }
    else
      ReadStreamToWriteStreamFsCopyTask(fromInfo = sourceFileInfo,
                                        toPath = realToPath,
                                        toDriver = targetDriver,
                                        skipIfCopyChildIsNotSupported = true,
                                        exportFormat = exportFormat,
                                        additionalParams = additionalParams)
  }
}