package com.intellij.aiplayground.ui.settings

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

interface LlmSettingsExtensionPoint<T> {
  val supportedType: Class<T>

  fun createSettingsConfigurable(project: Project, provider: LlmProvider, settings: T): LlmProviderSettingsConfigurable

  companion object {
    val EP_NAME: ExtensionPointName<LlmSettingsExtensionPoint<*>> = ExtensionPointName<LlmSettingsExtensionPoint<*>>("com.intellij.aiplayground.llmSettingsProvider")

    /**
     * Gets all registered LLM service extensions
     */
    fun getAllExtensions(): List<LlmSettingsExtensionPoint<*>> {
      return EP_NAME.extensionList
    }

    fun <T : LlmProviderSettings<T>> getProviderExtension(type: KClass<T>): LlmSettingsExtensionPoint<T> = getAllExtensions()
      .first { it.supportedType == type.java } as LlmSettingsExtensionPoint<T>
  }
} 