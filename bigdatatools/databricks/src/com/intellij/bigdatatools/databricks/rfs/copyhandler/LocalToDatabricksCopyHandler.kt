package com.intellij.bigdatatools.databricks.rfs.copyhandler

import com.intellij.bigdatatools.databricks.rfs.main.DatabricksWrapFileInfo
import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceDriver
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.copyhandler.InterDriverCopyHandler
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.task.ImportCopyTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.ReadStreamToWriteStreamFsCopyTask

class LocalToDatabricksCopyHandler : InterDriverCopyHandler {
  override fun canHandle(fromInfo: FileInfo,
                         toDriver: Driver): Boolean = fromInfo is LocalFileInfo && DbRfsUtils.isCorrectDriver(toDriver)

  override fun correctPathForTarget(fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver, exportFormat: ExportFormat?): RfsPath? =
    DbRfsUtils.correctPathForWorkspaceTarget(fromInfo, toPath, toDriver)

  override fun buildCopyTask(fromInfo: FileInfo,
                             toPath: RfsPath,
                             toDriver: Driver,
                             exportFormat: ExportFormat?,
                             additionalParams: Map<String, Any>) =
    if (isToWorkspace(toDriver, toPath)) {
      val extension = fromInfo.name.split(".").last()
      val format = exportFormat ?: DatabricksWorkspaceFileInfo.exportFormatByExtension(extension)
      if (format == null) {
        error("Files with extension '${extension}' isn't supported for upload to Databricks Workspace.")
      }
      ImportCopyTask(fromInfo, toPath, toDriver, skipIfCopyChildIsNotSupported = true,
                     format)
    }
    else
      ReadStreamToWriteStreamFsCopyTask(fromInfo, toPath, toDriver, skipIfCopyChildIsNotSupported = true, exportFormat,
                                        additionalParams = additionalParams)

  private fun isToWorkspace(toDriver: Driver,
                            toPath: RfsPath?) =
    toDriver is DatabricksWorkspaceDriver || toPath?.startsWith(DatabricksWrapFileInfo.WORKSPACE_ROOT_PATH) == true
}
