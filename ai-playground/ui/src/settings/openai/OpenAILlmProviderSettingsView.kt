package com.intellij.aiplayground.ui.settings.openai

import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.view.bind
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class OpenAILlmProviderSettingsView(
  parentScope: CoroutineScope,
  viewModel: OpenAILlmProviderSettingsViewModel,
  settings: Settings,
) : LlmProviderSettingsView<OpenAILlmProviderSettingsViewModel, OpenAILlmProviderSettingsView.Settings>(parentScope, viewModel, settings), Disposable {

  override fun Panel.contributeToForm() {
    row(AIPlaygroundUIBundle.message("api.key")) {
      passwordField().applyToComponent {
        document.whenTextChanged {
          viewModel.updateApiKey(it)
        }
        bind(coroutineScope, viewModel.settings.map { it.apiKey ?: "" })
      }.align(AlignX.FILL)
    }
    row(AIPlaygroundUIBundle.message("base.url")) {
      textField().applyToComponent {
        document.whenTextChanged {
          viewModel.updateBaseUrl(it)
        }
        bind(coroutineScope, viewModel.settings.map { it.endpoint ?: "" })
      }.align(AlignX.FILL)
    }.visible(settings.endpoint)
  }

  class Settings(
    dialog: Boolean,
    val endpoint: Boolean,
  ) : LlmProviderSettingsView.Settings(dialog)
}