package com.intellij.aiplayground.ui.settings.ollama

import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.view.bind
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView.Settings
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class OllamaLlmProviderSettingsView(
  parentScope: CoroutineScope,
  viewModel: OllamaLlmProviderSettingsViewModel,
  settings: Settings,
) : LlmProviderSettingsView<OllamaLlmProviderSettingsViewModel, Settings>(parentScope, viewModel, settings), Disposable {

  override fun Panel.contributeToForm() {
    row(AIPlaygroundUIBundle.message("endpoint")) {
      textField().applyToComponent {
        document.whenTextChanged {
          viewModel.updateEndpoint(it)
        }
        bind(coroutineScope, viewModel.settings.map { it.endpoint ?: "" })
      }.align(AlignX.FILL)
    }
  }
}