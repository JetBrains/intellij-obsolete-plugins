package com.intellij.bigdatatools.databricks.rfs.workspace

import com.databricks.sdk.service.workspace.Import
import com.databricks.sdk.service.workspace.ImportFormat
import com.databricks.sdk.service.workspace.Language
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.prefixIfNot
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.DriverBase
import com.jetbrains.bigdatatools.common.rfs.driver.DriverConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.ReadyConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import java.io.FileInputStream
import java.io.OutputStream
import java.util.Base64
import javax.swing.Icon

internal class DatabricksWorkspaceDriver(
  override val project: Project?,
  private val dbDriver: DatabricksDriver
) : DriverBase() {
  override val connectionData: ConnectionData
    get() = dbDriver.connectionData

  val client = dbDriver.dataManager.client
  override val presentableName: String = "Workspace"
  override val icon: Icon = dbDriver.icon

  init {
    Disposer.register(dbDriver, this)
  }

  override fun getExternalId(): String = dbDriver.getExternalId() + "_workspace"

  override fun validatePath(path: RfsPath): String? = null

  override suspend fun innerRefreshConnection(calledByUser: Boolean): ReadyConnectionStatus {
    return dbDriver.refreshConnection(if (calledByUser) ActivitySource.DATABRICKS_DELEGATE_USER else ActivitySource.DATABRICKS_DELEGATE)
  }

  override fun doIsAvailable(): DriverConnectionStatus = dbDriver.isAvailable()

  override fun doCheckAvailable() = error("should not be called")

  override fun doRefreshConnection(calledByUser: Boolean) = error("should not be called")

  override fun doGetHomeUri() = ""

  override fun doListStatus(path: RfsPath): List<FileInfo>? {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    val infos = client.workspaceList(stringPath) ?: return null
    return infos.map { info -> DatabricksWorkspaceFileInfo(this, info) }
  }

  override fun doGetFileStatus(path: RfsPath): FileInfo? {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    val info = client.workspaceFileInfo(stringPath) ?: return null

    return DatabricksWorkspaceFileInfo(this, info)
  }

  override fun doMkdir(path: RfsPath) {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    client.workspaceMakeDirs(stringPath)
  }

  override fun doCreateWriteStream(rfsPath: RfsPath, overwrite: Boolean, create: Boolean): OutputStream {
    return object : OutputStream() {
      private val byteChunks = mutableListOf<ByteArray>()

      override fun write(b: ByteArray, off: Int, len: Int) = write(b.sliceArray(IntRange(off, off + len)))

      override fun write(b: ByteArray) {
        byteChunks.add(b)
      }

      override fun flush() {
        //Ignore flush because we can convert just full file
      }

      override fun write(b: Int) = write(ByteArray(b))

      override fun close() {
        val totalLen = byteChunks.sumOf { it.size }
        val resBytes = ByteArray(totalLen)
        var curPos = 0
        byteChunks.forEach { chunk ->
          (curPos until (curPos + chunk.size)).forEach {
            resBytes[it] = chunk[it - curPos]
          }
          curPos += chunk.size
        }
        val encoded64 = Base64.getEncoder().encodeToString(resBytes)
        val savePath = rfsPath.stringRepresentation().prefixIfNot("/")

        val importInfo = Import()
          .setContent(encoded64)
          .setPath(savePath)
          .setFormat(ImportFormat.JUPYTER)
          .setOverwrite(overwrite)

        client.workspaceImport(importInfo)
      }
    }
  }

  override fun importFile(toPath: RfsPath, inputStream: FileInputStream, exportFormat: ExportFormat?) {
    val fileInfo = getFileStatus(toPath).result
    if (fileInfo != null) {
      val stringPath = toPath.stringRepresentation().prefixIfNot("/")
      client.workspaceDelete(stringPath, recursive = true)
    }

    val bytes = inputStream.readAllBytes()

    val encoded = Base64.getEncoder().encodeToString(bytes)

    val format = DatabricksWorkspaceFileInfo.exportFormatToDbImportFormat(exportFormat)
    val language = DbRfsUtils.getDbLanguage(exportFormat) ?: Language.PYTHON
    val importInfo = Import()
      .setContent(encoded)
      .setPath(toPath.stringRepresentation().prefixIfNot("/"))
      .setFormat(format)
      .setLanguage(language)
      .setOverwrite(true)
    client.workspaceImport(importInfo)
  }
}