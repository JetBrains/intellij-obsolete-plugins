package com.intellij.bigdatatools.databricks.rfs.copyhandler

import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.copyhandler.InterDriverCopyHandler
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalDriver
import com.jetbrains.bigdatatools.common.rfs.driver.task.ReadStreamToWriteStreamFsCopyTask

class DatabricksToLocalCopyHandler : InterDriverCopyHandler {
  override fun canHandle(fromInfo: FileInfo,
                         toDriver: Driver): Boolean = DbRfsUtils.isCorrectFileInfo(fromInfo) && toDriver is LocalDriver

  override fun buildCopyTask(fromInfo: FileInfo,
                             toPath: RfsPath,
                             toDriver: Driver,
                             exportFormat: ExportFormat?,
                             additionalParams: Map<String, Any>) =
    ReadStreamToWriteStreamFsCopyTask(fromInfo, toPath, toDriver, skipIfCopyChildIsNotSupported = true, exportFormat,
                                      additionalParams = additionalParams)
}