package com.intellij.aiplayground.ui.chat.editor

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatEvent
import com.intellij.aiplayground.models.chat.ChatId
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.ui.utils.getChatName
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class ChatVirtualFileHolder(private val project: Project, coroutineScope: CoroutineScope) {
  private val chatFiles = mutableMapOf<ChatId, ChatVirtualFile>()

  init {
    coroutineScope.launch {
      project.service<ChatRepository>().getEventsFlow().collect { event ->
        when (event) {
          is ChatEvent.ChatUpdated -> {
            chatFiles[event.chat.id]?.let {
              val oldName = it.fileName
              it.chat = event.chat
              if (oldName != event.chat.title) {
                edtWriteAction {
                  it.rename(this, getChatName(event.chat.title))
                }
              }
            }
          }
          is ChatEvent.ChatCreated -> {}
          is ChatEvent.ChatRemoved -> {
            chatFiles[event.chat.id]?.let {
              edtWriteAction {
                it.delete(this)
                chatFiles.remove(event.chat.id)
              }
            }
          }
        }
      }
    }
  }

  fun getOrCreate(chat: Chat): VirtualFile {
    return chatFiles.getOrPut(chat.id) {
      ChatVirtualFile(project, chat, getChatName(chat.title))
    }
  }

  companion object {
    fun getInstance(project: Project): ChatVirtualFileHolder = project.service()
  }
}