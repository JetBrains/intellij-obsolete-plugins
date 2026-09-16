package com.intellij.bigdatatools.databricks.rfs.dbfs

import com.intellij.bigdatatools.databricks.model.DbfsFile
import com.intellij.bigdatatools.databricks.rfs.dbfs.task.DbfsMoveTask
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.bigdatatools.coreUi.util.prefixIfNot
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfoBase
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsDeleteTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import java.io.InputStream

internal class DbfsFileInfo(override val driver: DbfsDriver, private val info: DbfsFile) : FileInfoBase() {
  internal val client = driver.client

  override val path: RfsPath = let {
    val clearPath = info.path.removePrefix("/").removeSuffix("/")
    val stringPath = clearPath + if (info.isDir && info.path != "/") "/" else ""
    driver.createRfsPath(stringPath)
  }

  override val externalPath: String = "dbfs:/" + path.stringRepresentation()
  override val name: String = path.name
  override val length: Long = info.fileSize
  override val modificationTime: Long = info.modificationTime

  override fun doRenameAsync(newPath: RfsPath, overwrite: Boolean): RfsCopyMoveTask = DbfsMoveTask(this, newPath)

  override fun doDeleteAsync() = object : RemoteFsDeleteTask(path) {
    override fun run(indicator: ProgressIndicator) {
      val stringPath = path.stringRepresentation().prefixIfNot("/")
      client.dbfsDelete(stringPath, recursive = true)
    }
  }

  override fun doGetReadStream(offset: Long, exportFormat: ExportFormat?): InputStream {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    return client.dbfsInputStream(stringPath)
  }

  override val isCopySupport: Boolean = true

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DbfsFileInfo) return false
    if (!super.equals(other)) return false

    return info == other.info
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + info.hashCode()
    return result
  }
}

