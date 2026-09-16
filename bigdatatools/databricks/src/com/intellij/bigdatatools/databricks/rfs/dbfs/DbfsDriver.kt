package com.intellij.bigdatatools.databricks.rfs.dbfs

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.prefixIfNot
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.DriverBase
import com.jetbrains.bigdatatools.common.rfs.driver.DriverConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.ReadyConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import java.io.OutputStream
import javax.swing.Icon

internal class DbfsDriver(
  override val project: Project?,
  private val dbDriver: DatabricksDriver
) : DriverBase() {
  override val connectionData: ConnectionData
    get() = dbDriver.connectionData
  val dataManager = dbDriver.dataManager
  val client = dataManager.client
  override val presentableName: String = "Dbfs"
  override val icon: Icon = dbDriver.icon

  init {
    Disposer.register(dbDriver, this)
  }

  override fun getExternalId(): String = dbDriver.getExternalId() + "_dbfs"

  override fun validatePath(path: RfsPath): String? = null

  override fun doIsAvailable(): DriverConnectionStatus = dbDriver.isAvailable()

  override suspend fun innerRefreshConnection(calledByUser: Boolean): ReadyConnectionStatus {
    return dbDriver.refreshConnection(if (calledByUser) ActivitySource.DATABRICKS_DELEGATE_USER else ActivitySource.DATABRICKS_DELEGATE)
  }

  override fun doCheckAvailable() = error("should not be called")

  override fun doRefreshConnection(calledByUser: Boolean) = error("should not be called")

  override fun doCreateWriteStream(rfsPath: RfsPath, overwrite: Boolean, create: Boolean): OutputStream {
    val stringPath = rfsPath.stringRepresentation().prefixIfNot("/")
    return client.dbfsOutputStream(stringPath, create = true)
  }

  override fun doGetHomeUri() = ""

  override fun doListStatus(path: RfsPath): List<FileInfo> {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    val infos = client.dbfsList(stringPath)
    return infos.map { info -> DbfsFileInfo(this, info) }
  }

  override fun doGetFileStatus(path: RfsPath): FileInfo {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    val info = client.dbfsGetStatus(stringPath)
    return DbfsFileInfo(this, info)
  }

  override fun doMkdir(path: RfsPath) {
    val stringPath = path.stringRepresentation().prefixIfNot("/")
    client.dbfsMakeDirs(stringPath)
  }
}