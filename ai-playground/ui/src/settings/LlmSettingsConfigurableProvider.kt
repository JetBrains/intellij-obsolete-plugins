package com.intellij.aiplayground.ui.settings

import com.intellij.aiplayground.ui.utils.isChinaRegion
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class LlmSettingsConfigurableProvider(private val project: Project): ConfigurableProvider() {
  override fun createConfigurable(): Configurable {
    return LlmSettingsConfigurable(project)
  }

  override fun canCreateConfigurable(): Boolean {
    return !isChinaRegion()
  }
}