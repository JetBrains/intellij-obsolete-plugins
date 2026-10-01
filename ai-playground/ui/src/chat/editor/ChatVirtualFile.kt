package com.intellij.aiplayground.ui.chat.editor

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatId
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFileWithAssignedFileType
import com.intellij.openapi.vfs.VirtualFileWithoutContent
import org.jetbrains.annotations.NonNls
import java.io.InputStream
import java.io.OutputStream

class ChatVirtualFile(val project: Project, var chat: Chat, var fileName: String) : VirtualFile(), VirtualFileWithAssignedFileType, VirtualFileWithoutContent {
  override fun getName(): @NlsSafe String = fileName

  override fun getFileSystem(): VirtualFileSystem = PlaygroundFileSystem.getInstance()

  override fun getPath(): @NonNls String = chatFileName(project, chat.id)

  override fun isWritable(): Boolean = true

  override fun isDirectory(): Boolean = false

  override fun isValid(): Boolean = true

  override fun getParent(): VirtualFile? = null

  override fun getChildren(): Array<out VirtualFile?>? = null

  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream = throw UnsupportedOperationException()

  override fun contentsToByteArray(): ByteArray = throw UnsupportedOperationException()

  override fun getTimeStamp(): Long = chat.updatedAt

  override fun getLength(): Long = 0

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

  override fun getInputStream(): InputStream = throw UnsupportedOperationException()

  override fun getAssignedFileType(): FileType = ChatFileType

  override fun getModificationStamp(): Long = chat.updatedAt

}

private object ChatFileType : FileType {
  override fun getName(): @NonNls String = "aiPlaygroundChatType"

  override fun getDescription(): @NlsContexts.Label String = "AIPlaygroundChatFile"
  override fun getDefaultExtension() = ""

  override fun getIcon() = AllIcons.General.Settings
  override fun isBinary(): Boolean = true
  override fun isReadOnly(): Boolean = true
}

private fun chatFileName(project: Project, id: ChatId): String {
  return "${project.locationHash}/${id.id}"
}
