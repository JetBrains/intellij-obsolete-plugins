package com.intellij.aiplayground.ui.chat.editor

import com.intellij.aiplayground.models.chat.ChatId
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent


class PlaygroundFileSystem() : DeprecatedVirtualFileSystem() {

  companion object {
    const val PROTOCOL: String = "ai-playground"

    fun getInstance(): PlaygroundFileSystem {
      return VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as PlaygroundFileSystem
    }
  }

  override fun findFileByPath(path: String): VirtualFile? {
    val split = path.split("/").filterNot(String::isNullOrBlank)
    return ProjectManager.getInstance().openProjects.firstOrNull {
      it.locationHash == split.getOrNull(0)
    }?.let { project ->
      split.getOrNull(1)?.let { id ->
        project.service<ChatRepository>().getChat(ChatId(id))?.let { chat ->
          ChatVirtualFileHolder.getInstance(project).getOrCreate(chat)
        }
      }
    }
  }

  override fun refresh(asynchronous: Boolean) {
  }

  override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

  override fun getProtocol(): String {
    return PROTOCOL
  }

  override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
    val oldName = vFile.getName()
    fireBeforePropertyChange(requestor, vFile, VirtualFile.PROP_NAME, oldName, newName)
    wrapWithEvent(VFilePropertyChangeEvent(this, vFile, VirtualFile.PROP_NAME, oldName, newName)) {
      (vFile as ChatVirtualFile).fileName = newName
    }
    firePropertyChanged(requestor, vFile, VirtualFile.PROP_NAME, oldName, newName)
  }

  override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
    fireBeforeFileDeletion(requestor, vFile)
    wrapWithEvent(VFileDeleteEvent(this, vFile)) {}
    fireFileDeleted(requestor, vFile, vFile.getName(), null)
  }

  fun wrapWithEvent(event: VFileEvent, block: () -> Unit) {
    val listener = ApplicationManager.getApplication().getMessageBus().syncPublisher(VirtualFileManager.VFS_CHANGES)
    listener.before(listOf(event))
    block()
    listener.after(listOf(event))
  }
}
