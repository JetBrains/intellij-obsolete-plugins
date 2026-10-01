package com.intellij.aiplayground.ui.utils

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.ui.AIPlaygroundIconsHelper
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.swing.Icon

fun getModelIcon(project: Project, instanceId: LlmProviderInstanceId, modelId: LlmModelId): Flow<Icon> {
  return flow {
    project.service<LlmServiceManager>().configuredProviders.firstOrNull { it.id.id == instanceId.id }?.let { instance ->
      project.service<LlmServiceManager>().getModel(instance, modelId).let { model ->
        emit(AIPlaygroundIconsHelper.providerName2Icon(model.provider.id.id))
      }
    }
  }
}