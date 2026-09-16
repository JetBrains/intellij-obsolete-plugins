@file:Suppress("DuplicatedCode")

package com.intellij.bigdatatools.zeppelin.components.instance.service

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.ErrorResult
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.OkResult
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.SafeResult
import com.jetbrains.bigdatatools.common.rfs.driver.flowOfSingleInterruptible
import com.jetbrains.bigdatatools.common.rfs.fileInfo.DriverFileInfoManager
import com.jetbrains.bigdatatools.common.rfs.fileInfo.DriverRfsListener
import com.jetbrains.bigdatatools.common.rfs.fileInfo.RfsChildrenPartId
import com.jetbrains.bigdatatools.common.rfs.fileInfo.RfsFileInfoChildren
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class ZeppelinFileManager(override val driver: ZeppelinDriver) : DriverFileInfoManager() {
  val project = driver.project
  private val connectionManager = driver.connectionManager
  private val connection = connectionManager.instanceConnection

  private var notePaths: List<ZeppelinRfsPath>? = connection.cachedNotesInfo?.map { ZeppelinRfsPath.createFromNoteInfo(it) } ?: emptyList()

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      isConnecting.set(true)
      notePaths = connection.cachedNotesInfo?.map { ZeppelinRfsPath.createFromNoteInfo(it) }

      if (notePaths != null)
        isConnecting.set(false)

      refreshTree()
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      isConnecting.set(false)
      notePaths = emptyList()

      driver.notify {
        it.nodeUpdated(driver.root)
      }

      if (!isReloading.get())
        notify {
          it.treeUpdated(driver.root)
        }
    }

    override fun updateNotebookList(notebooks: List<NotebookInfo>) {
      isConnecting.set(false)
      notePaths = notebooks.map { ZeppelinRfsPath.createFromNoteInfo(it) }


      refreshTree()
    }
  }

  init {
    connection.addListener(connectionListener)
  }

  override fun dispose() = connection.removeListener(connectionListener)

  override fun notify(body: (DriverRfsListener) -> Unit) {
    driver.notify(body)
  }

  override fun refreshFiles(path: RfsPath) {
    if (connection.isConnected())
      driver.fileSystem.refreshNotes()
    else
      notify {
        it.treeUpdated(path)
      }
  }

  override suspend fun refreshDriver(activitySource: ActivitySource) {
    notePaths = null
    invokeDriverRefresh(activitySource)
  }

  override fun getCachedFileInfoInner(rfsPath: RfsPath): SafeResult<FileInfo?> {
    if (!driver.connectionManager.isConnected()) {
      return ErrorResult(Exception(ZepMessagesBundle.message("connection.error")))
    }
    if (rfsPath.isRoot && notePaths != null)
      return OkResult(ZeppelinFileInfo(driver, ZeppelinRfsPath.fromRfsPath(rfsPath)))

    val isContains = if (rfsPath.isFile)
      notePaths?.any { it.serverPath == (ZeppelinRfsPath.fromRfsPath(rfsPath)).serverPath } == true
    else
      notePaths?.any { it.startsWith(rfsPath) } == true || rfsPath.isRoot
    val fileInfo = if (isContains)
      ZeppelinFileInfo(driver, ZeppelinRfsPath.fromRfsPath(rfsPath))
    else
      null
    return OkResult(fileInfo)
  }

  override fun getCachedChildrenInner(rfsPath: RfsPath): SafeResult<RfsFileInfoChildren> = getChildren(rfsPath)

  override fun getChildren(pageKey: RfsChildrenPartId, force: Boolean): Flow<SafeResult<RfsFileInfoChildren>> {
    return flowOfSingleInterruptible { getChildren(pageKey.rfsPath, force) }
  }

  override fun getChildren(rfsPath: RfsPath, force: Boolean): SafeResult<RfsFileInfoChildren> {
    if (!driver.connectionManager.isConnected()) {
      return ErrorResult(Exception(ZepMessagesBundle.message("connection.error")))
    }

    val children = notePaths
                     ?.filter { it.startsWith(rfsPath) }
                     ?.map { it.prefixPath(rfsPath.nameCount + 1) }
                     ?.distinct() ?: emptyList()
    val paths = children.sortedWith(compareBy({ it.isTrash }, { !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))

    return OkResult(RfsFileInfoChildren(paths.map { ZeppelinFileInfo(driver, it) }))
  }

  override fun invokeLoadFileInfo(rfsPath: RfsPath) {
  }

  override fun loadFileInfo(rfsPath: RfsPath, force: Boolean) = getCachedFileInfoInner(rfsPath)

  fun refreshTree() = notify {
    it.treeUpdated(driver.root)
  }

  override suspend fun getMetaFileInfo(fileInfo: FileInfo, project: Project): SafeResult<List<SchemaInfoPart>?> = OkResult(null)
  override fun hasMetaInfo(rfsPath: RfsPath, project: Project): Boolean = false

  override fun waitAppear(rfsPath: RfsPath) {
    driver.fileSystem.refreshNotes()
  }

  override fun waitDisappear(rfsPath: RfsPath) {
    driver.fileSystem.refreshNotes()
  }

  override fun getDriverConnectionStatus() = driver.fileSystem.getConnectionStatus()
}