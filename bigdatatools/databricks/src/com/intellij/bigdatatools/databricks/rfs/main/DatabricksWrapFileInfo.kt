package com.intellij.bigdatatools.databricks.rfs.main

import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.rfs.dbfs.DbfsFileInfo
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.SafeResult
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsMoveTask

internal class DatabricksWrapFileInfo(private val dbDriver: DatabricksDriver, val sourceFileInfo: FileInfo) : FileInfo by sourceFileInfo {
  override val name: String
    get() = when {
      sourceFileInfo.path.isRoot && sourceFileInfo is DbfsFileInfo -> DBFS_PREFIX
      sourceFileInfo.path.isRoot && sourceFileInfo is DatabricksWorkspaceFileInfo -> WORKSPACE_PREFIX
      else -> sourceFileInfo.name
    }

  override val path: RfsPath = let {
    val prefixRfsPath = if (sourceFileInfo is DbfsFileInfo)
      DBFS_ROOT_PATH
    else
      WORKSPACE_ROOT_PATH

    sourceFileInfo.path.replacePrefix(0, prefixRfsPath)
  }

  override val driver: Driver
    get() = dbDriver

  override fun renameAsync(newPath: RfsPath, overwrite: Boolean): SafeResult<RemoteFsMoveTask> = sourceFileInfo.renameAsync(
    newPath.dropPrefix(1), overwrite)

  override fun toString(): String = "DatabricksWrapFileInfo(path=$path)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DatabricksWrapFileInfo) return false

    return sourceFileInfo == other.sourceFileInfo
  }

  override fun hashCode(): Int = sourceFileInfo.hashCode()


  companion object {
    private const val DBFS_PREFIX = "DBFS"
    internal const val WORKSPACE_PREFIX = "Workspace"

    val DBFS_ROOT_PATH = RfsPath(listOf(DBFS_PREFIX), true)
    val WORKSPACE_ROOT_PATH = RfsPath(listOf(WORKSPACE_PREFIX), true)
  }
}