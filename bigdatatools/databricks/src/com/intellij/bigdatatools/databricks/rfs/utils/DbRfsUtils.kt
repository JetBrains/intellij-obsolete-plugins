package com.intellij.bigdatatools.databricks.rfs.utils

import com.databricks.sdk.service.workspace.Language
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.rfs.dbfs.DbfsDriver
import com.intellij.bigdatatools.databricks.rfs.dbfs.DbfsFileInfo
import com.intellij.bigdatatools.databricks.rfs.main.DatabricksWrapFileInfo
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceDriver
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

internal object DbRfsUtils {
  fun isCorrectFileInfo(fileInfo: FileInfo) = fileInfo is DatabricksWrapFileInfo || fileInfo is DbfsFileInfo || fileInfo is DatabricksWorkspaceFileInfo
  fun isCorrectDriver(driver: Driver) = driver is DatabricksDriver || driver is DbfsDriver || driver is DatabricksWorkspaceDriver

  fun getDbLanguage(exportFormat: ExportFormat?): Language? {
    exportFormat ?: return null
    if (exportFormat != DatabricksWorkspaceFileInfo.SOURCE_EXPORT_FORMAT) {
      return null
    }

    return when (exportFormat.extension.removePrefix(".")) {
      "py" -> Language.PYTHON
      "sql" -> Language.SQL
      "scala" -> Language.SCALA
      "r" -> Language.R
      else -> null
    }
  }

  fun getExtensionByDbNoteLanguage(exportFormat: Language): String = "." + when (exportFormat) {
    Language.SCALA -> "scala"
    Language.PYTHON -> "py"
    Language.SQL -> "sql"
    Language.R -> "r"
  }

  fun correctPathForWorkspaceTarget(fromInfo: FileInfo, toPath: RfsPath, toDriver: Driver): RfsPath? {
    val isToWorkspace = toPath.startsWith(DatabricksWrapFileInfo.WORKSPACE_ROOT_PATH) || toDriver is DatabricksWorkspaceDriver

    return if (fromInfo.originInfo !is DatabricksWorkspaceFileInfo && isToWorkspace && fromInfo.isFile) {
      val name = fromInfo.name
      val extension = "." + name.substringAfterLast('.', "")
      toPath.child(name.removeSuffix(extension), isDirectory = false)
    }
    else {
      null
    }
  }


  val FileInfo.originInfo
    get() = if (this is DatabricksWrapFileInfo)
      sourceFileInfo
    else
      this
}