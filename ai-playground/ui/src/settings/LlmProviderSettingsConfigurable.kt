package com.intellij.aiplayground.ui.settings

import com.intellij.aiplayground.models.settings.LlmProviderSettings
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JComponent

interface LlmProviderSettingsConfigurable {
  fun dispose()
  fun createComponent(): JComponent
  fun createDialogComponent(): JComponent
  fun getSettings(): StateFlow<LlmProviderSettings<*>>
}