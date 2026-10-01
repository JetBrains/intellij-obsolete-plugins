package com.intellij.aidebugger.koog.execution

import com.intellij.aidebugger.common.AiDebuggerIcons
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.koog.AiDebuggerKoogBundle
import com.intellij.execution.Executor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import javax.swing.Icon

/**
 * The executor for running configurations with Koog agents with attach to AI Debugger.
 *
 * This executor integrates with the IntelliJ Platform's execution framework to provide
 * AI-powered debugging capabilities. It appears as a run action alongside standard
 * Run and Debug executors when enabled via the global settings.
 *
 * NOTE: The executor is disabled by default.
 * Please use the [GlobalSettingsService.showExtraDebugButton] registry setting to enable it.
 */
class KoogAgentExecutor : Executor() {
    companion object {
        const val EXECUTOR_ID = "KoogTemporaryExecutor"
    }

    override fun getToolWindowId(): String = ToolWindowId.DEBUG

    override fun getToolWindowIcon(): Icon = AiDebuggerIcons.TOOLWINDOW_ICON

    override fun getIcon(): Icon = AiDebuggerIcons.TOOLWINDOW_ICON

    override fun getDisabledIcon(): Icon = AiDebuggerIcons.TOOLWINDOW_ICON

    override fun getDescription(): String =
        AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.executor.description", )

    override fun getActionName(): String =
        AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.executor.start.action.text", )

    override fun getId(): String = EXECUTOR_ID

    override fun getStartActionText(): String =
        AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.executor.start.action.text", )

    override fun getStartActionText(configurationName: String): String =
        AiDebuggerKoogBundle.message(
            "aitoolkit.debugger.koog.agent.executor.start.action.with.configuration.text",
            configurationName
        )

    override fun getContextActionId(): String = "RunAiDebuggerKoogAgentAction"

    override fun getHelpId(): String = "helpID"

    /**
     * Determines whether the executor is applicable in the given project context
     * based on the global settings configuration.
     *
     * @param project the instance of the current project where the executor is being checked.
     * @return true if the extra debug button setting is enabled via [GlobalSettingsService], false otherwise.
     */
    override fun isApplicable(project: Project): Boolean {
        return GlobalSettingsService.getInstance().showExtraDebugButton.value
    }
}