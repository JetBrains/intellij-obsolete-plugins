package com.intellij.bigdatatools.zeppelin.file

import com.intellij.bigdatatools.zeppelin.models.ZeppelinVirtualFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.testFramework.LightVirtualFile

open class ZeppelinRemoteFile(name: String, text: CharSequence) : LightVirtualFile(name, ZeppelinFileType, text) {
  private var isWriteAllowed = true

  val noteId
    get() = NotebookFileUtil.getNotebookId(this) ?: error("Note id is not found for file $name")

  private val connectionId
    get() = NotebookFileUtil.getConfigId(this) ?: error("Config id is not found for file $name")

  fun andCache(): ZeppelinRemoteFile {
    ZeppelinVirtualFileSystem.cacheZeppelinVirtualFile(this)
    return this
  }

  override fun isWritable(): Boolean = isWriteAllowed

  override fun setWritable(writable: Boolean) {
    isWriteAllowed = writable
  }

  override fun getFileSystem(): VirtualFileSystem = ZeppelinVirtualFileSystem.VFS_INSTANCE

  override fun getPath(): String = "$connectionId:$noteId:$name"

  override fun getParent(): VirtualFile = parentForRemote

  override fun toString(): String = "ZeppelinRemoteFile: ${path}"

  override fun equals(other: Any?): Boolean {
    return other is ZeppelinRemoteFile && other.connectionId == connectionId && other.noteId == noteId
  }

  override fun hashCode(): Int = 31 * noteId.hashCode() + connectionId.hashCode()

  companion object {
    val parentForRemote = object : LightVirtualFile("ParentForZeppelinRemote") {
      override fun isDirectory(): Boolean = true
      override fun isWritable(): Boolean = false
      override fun getFileSystem(): VirtualFileSystem = ZeppelinVirtualFileSystem.VFS_INSTANCE
      override fun getPath(): String = "/"
    }
  }
}