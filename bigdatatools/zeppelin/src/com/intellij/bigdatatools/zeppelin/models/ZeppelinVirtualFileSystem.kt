package com.intellij.bigdatatools.zeppelin.models

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinFileTypeViewer
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import java.lang.ref.SoftReference

class ZeppelinVirtualFileSystem : VirtualFileSystem() {
  companion object {
    const val ZEPPELIN_PROTOCOL = "zeppelin"

    private val cachedFiles = HashMap<String, SoftReference<VirtualFile>>() //

    private const val IS_CACHING_ENABLED = false // todo something with it when synchronisation works

    fun cacheZeppelinVirtualFile(file: VirtualFile) {
      if (IS_CACHING_ENABLED) cachedFiles[file.path] = SoftReference(file)
    }

    val VFS_INSTANCE = ZeppelinVirtualFileSystem()

    fun findDisplayName(zeppelinFile: VirtualFile): String =
      if (zeppelinFile !is ZeppelinRemoteFile)
        zeppelinFile.name
      else
        extractFileInfo(zeppelinFile)?.let { info ->
          findDisplayName(info)
        } ?: zeppelinFile.name

    //fun checkFileWasRemoteDeleted(zeppelinRemoteFile: ZeppelinRemoteFile): Boolean {
    //  if (isOpened(zeppelinRemoteFile)) return false
    //
    //  val extractedFileInfo = extractFileInfo(zeppelinRemoteFile) ?: return true
    //  val driver = getDriver(extractedFileInfo.connId) ?: return true
    //  //TODO this used to be driver.isAvailable() and semantically still is, but we do not want safeExecute() here
    //  val driverConnectionStatus = driver.fileSystem.getConnectionStatus()
    //  return driverConnectionStatus.isConnected() && driver.getNotePathById(extractedFileInfo.noteId) == null
    //}

    //private fun isOpened(zeppelinRemoteFile: ZeppelinRemoteFile): Boolean {
    //  val allProjects = ProjectManager.getInstance().openProjects
    //  val openedEditors = allProjects.flatMap { FileEditorManager.getInstance(it).openFiles.toList() }
    //  return openedEditors.any { it.getOriginalFile() == zeppelinRemoteFile }
    //}

    private fun findDisplayName(info: ExtractedFileInfo): String =
      getDriver(info.connId)?.getNotePathById(info.noteId)?.name ?: info.name

    private fun getDriver(connId: String): ZeppelinDriver? =
      DriverManager.getDriversForAllOpenProjects()
        .find { driver -> driver is ZeppelinDriver && driver.getExternalId() == connId } as? ZeppelinDriver

    private fun extractFileInfo(vFilePath: String): ExtractedFileInfo? {
      val sp = vFilePath.split(':')
      return if (sp.size != 3) null else ExtractedFileInfo(sp[0], sp[1], sp[2])
    }

    private fun extractFileInfo(vFile: VirtualFile): ExtractedFileInfo? = extractFileInfo(vFile.path)

    private data class ExtractedFileInfo(val connId: String, val noteId: String, val name: String)
  }

  private val listeners = mutableSetOf<VirtualFileListener>()

  override fun getProtocol(): String = ZEPPELIN_PROTOCOL

  // we'll assume path looks like connection_id:note_id:name
  override fun findFileByPath(path: String): VirtualFile? {
    val extractedFileInfo = extractFileInfo(path) ?: return null
    val driver = getDriver(extractedFileInfo.connId)
    if (driver == null) return null
    return ZeppelinFileTypeViewer.Util.prepareVirtualFile(driver.project
                                                     ?: ProjectManager.getInstance().openProjects.firstOrNull()
                                                     ?: ProjectManager.getInstance().defaultProject,
                                                     extractedFileInfo.name, extractedFileInfo.noteId, extractedFileInfo.connId)
  }

  override fun extractPresentableUrl(path: String): String {
    val extractedFileInfo = extractFileInfo(path) ?: return super.extractPresentableUrl(path)
    val displayName = findDisplayName(extractedFileInfo)
    val conn = RfsConnectionDataManager.instance?.findConnectionInAllProjects(extractedFileInfo.connId) ?: return displayName

    return "${conn.name}@{$displayName}"
  }

  override fun refresh(asynchronous: Boolean) =/*todo?*/Unit

  override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

  override fun addVirtualFileListener(listener: VirtualFileListener) {
    listeners.add(listener)
  }

  override fun removeVirtualFileListener(listener: VirtualFileListener) {
    listeners.remove(listener)
  }

  override fun deleteFile(requestor: Any?, vFile: VirtualFile) {}
  override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {}
  override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {}
  override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile = virtualFile
  override fun isReadOnly(): Boolean = false

  override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile = throw NotImplementedError() /*todo*/
  override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile = throw NotImplementedError() /*todo*/
}