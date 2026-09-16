package com.intellij.bigdatatools.zeppelin.drivers.fileinfo

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinFileTypeViewer
import com.intellij.bigdatatools.zeppelin.drivers.tasks.ZeppelinMoveTask
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfoBase
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.SafeResult
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsDeleteTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import com.jetbrains.bigdatatools.common.rfs.fileInfo.LazyFileInfoInputStream
import java.io.InputStream

class ZeppelinFileInfo(override val driver: ZeppelinDriver, override val path: ZeppelinRfsPath) : FileInfoBase() {
  val isTrashRoot: Boolean = "/${ZeppelinConstants.TRASH_NAME}/" == path.serverPath
  val isInTrash: Boolean = isTrashRoot || path.isInTrash

  override val length: Long = -1
  override val modificationTime: Long = 20
  override val name: String = if (isTrashRoot) "Trash" else path.name
  override val externalPath: String = "${driver.doGetHomeUri()}${path.canonicalPath}"
  override val isCopySupport: Boolean = !isTrashRoot
  override val isActionDeleteSupport: Boolean = false

  val noteUrl: String
    get() {
      val httpUrl = driver.connectionManager.tunnelUri ?: driver.connectionData.getFullHttpUrl()
      return ZeppelinConnectionData.getUrlToNote(httpUrl, path.id)
    }

  @RequiresBackgroundThread
  fun removeFromTrash() = safeExecute { driver.fileSystem.deleteFromTrash(path) }

  @RequiresBackgroundThread
  fun clearOutput() = safeExecute { driver.fileSystem.clearOutput(path) }

  @RequiresBackgroundThread
  fun restore(): SafeResult<Unit> = safeExecute { driver.fileSystem.restoreFromTrash(path) }

  fun openPsiFile(requestFocus: Boolean, project: Project) =
    ZeppelinFileTypeViewer.Util.INSTANCE.openFile(project, driver, path, requestFocus)

  override fun doRenameAsync(newPath: RfsPath, overwrite: Boolean): RfsCopyMoveTask = ZeppelinMoveTask(this, newPath)

  override fun doDeleteAsync(): RemoteFsTask = object : RemoteFsDeleteTask(path) {
    override fun run(indicator: ProgressIndicator) = driver.fileSystem.moveToTrash(path)
  }

  override fun doGetReadStream(offset: Long, exportFormat: ExportFormat?): InputStream = object : LazyFileInfoInputStream() {
    override fun initStream(): InputStream {
      val note = driver.exportNote(path)?.note ?: error("Cannot export note $path")
      return note.asJson().byteInputStream()
    }
  }

  override val isSynthetic: Boolean get() = isDirectory && !isTrashRoot
  override fun isMetaInfoSupport(): Boolean = false
  override fun isMkDirSupport(): Boolean = false
  override fun isCreateFileSupport(): Boolean = false

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ZeppelinFileInfo) return false

    if (path != other.path) return false
    if (driver != other.driver) return false

    return true
  }

  override fun hashCode(): Int {
    var result = path.hashCode()
    result = 31 * result + driver.hashCode()
    return result
  }
}

