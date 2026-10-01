package com.intellij.aidebugger.evaluation.settings

import com.intellij.aidebugger.evaluation.settings.viewmodels.AIToolkitSettingsViewModel
import com.intellij.aidebugger.evaluation.settings.views.AIToolkitSettingsView
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent

/**
 * Settings configurable for AI Toolkit LLM provider credentials.
 * Supports multiple credential instances per provider type.
 */
class AIToolkitSettingsConfigurable(val project: Project) : SearchableConfigurable, Configurable.NoScroll {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val viewModel: AIToolkitSettingsViewModel by lazy { AIToolkitSettingsViewModel(project, scope) }
    private val view: AIToolkitSettingsView by lazy { AIToolkitSettingsView(project, scope, viewModel) }

    override fun getId(): @NonNls String = "com.intellij.aidebugger.settings.llm"

    override fun createComponent(): JComponent {
        return view
    }

    override fun isModified(): Boolean {
        return viewModel.isModified() ?: false
    }

    override fun apply() {
        viewModel.apply()
    }

    override fun reset() {
        viewModel.reset()
    }

    override fun getDisplayName(): String = AIToolkitUIBundle.message("configurable.ai.toolkit.display.name")

    override fun getPreferredFocusedComponent(): JComponent {
        return view
    }

    override fun disposeUIResources() {
        view.let { Disposer.dispose(it) }
        viewModel.dispose()
        scope.cancel()
    }
}
