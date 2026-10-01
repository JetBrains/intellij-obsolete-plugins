package com.intellij.aidebugger.evaluation.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class AIToolkitSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return AIToolkitSettingsConfigurable(project)
    }

    override fun canCreateConfigurable(): Boolean {
        return !isChinaRegion()
    }
}