package com.intellij.aiplayground.ui.utils

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun getModelName(project: Project, instanceId: LlmProviderInstanceId, modelId: LlmModelId): Flow<@NlsSafe String> {
  return flow {
    project.service<LlmServiceManager>().configuredProviders.firstOrNull { it.id.id == instanceId.id }?.let { instance ->
      project.service<LlmServiceManager>().getModel(instance, modelId).let { model ->
        emit("${instance.settings.displayName} - ${model.displayName}")
      }
    }
  }
}