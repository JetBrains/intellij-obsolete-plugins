package com.intellij.aiplayground.aiassistant

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AIAssistantModelsProvider {
  suspend fun getSupportedModelsNames(project: Project): List<String>

  suspend fun makeRequest(project: Project, modelName: String, messages: List<PlaygroundChatMessage>, parameters: PlaygroundParameters, handler: PlaygroundStreamingChatResponseHandler)

  fun isAvailable(project: Project): StateFlow<Boolean>

  companion object {
    val EP_NAME: ExtensionPointName<AIAssistantModelsProvider> = ExtensionPointName<AIAssistantModelsProvider>("com.intellij.aiplayground.aiAssistantModelsProvider")

    suspend fun getSupportedModelsNames(project: Project): List<String>? {
      return EP_NAME.extensionList.firstOrNull()?.getSupportedModelsNames(project)
    }

    suspend fun makeRequest(project: Project, modelName: String, messages: List<PlaygroundChatMessage>, parameters: PlaygroundParameters, handler: PlaygroundStreamingChatResponseHandler) {
      EP_NAME.extensionList.firstOrNull()?.makeRequest(project, modelName, messages, parameters, handler)
    }

    fun isAvailable(project: Project): StateFlow<Boolean> {
      val provider = EP_NAME.extensionList.firstOrNull() ?: return MutableStateFlow(false)
      return try {
        provider.isAvailable(project)
      }
      catch (t: Throwable) {
        MutableStateFlow(false)
      }
    }
  }
}
