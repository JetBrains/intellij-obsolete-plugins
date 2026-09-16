package com.intellij.bigdatatools.plugin.spark.submit.gutter

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ExecutorRegistryImpl.RunnerHelper
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.EditConfigurationsDialog
import com.intellij.execution.impl.ProjectRunConfigurationConfigurable
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil.INLINE_ACTIONS
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.ui.icons.toStrokeIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration

internal class SubmitConfigurationAction(
  private val project: Project,
  val configurationSettings: RunnerAndConfigurationSettings
) : DefaultActionGroup() {
  val configuration: ClusterSparkJobRunConfiguration = configurationSettings.configuration as ClusterSparkJobRunConfiguration
  init {
    isPopup = true
    templatePresentation.isPerformGroup = true
    templatePresentation.text = configuration.name
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val executors = listOfNotNull(
      DefaultRunExecutor.EXECUTOR_ID,
      ToolWindowId.DEBUG.takeIf { !configuration.factory.isPySpark }
    )
    val inlineActions = executors.mapNotNull { executorId ->
      val executor = ExecutorRegistry.getInstance().getExecutorById(executorId) ?: return@mapNotNull null
      val enabled = RunnerHelper.canRun(project, executor, configuration)
      SubmitRunAction(configurationSettings, executor, enabled)
    }
    e.presentation.putClientProperty(INLINE_ACTIONS, inlineActions)
  }

  override fun actionPerformed(e: AnActionEvent) {
    EditConfigurationsDialog(project, object : ProjectRunConfigurationConfigurable(project) {
      override fun getInitialSelectedConfiguration() = configurationSettings
    }, e.dataContext).show()
  }
}

private class SubmitRunAction(
  private val configurationSettings: RunnerAndConfigurationSettings,
  private val executor: Executor,
  val enabled: Boolean
) : DumbAwareAction() {
  init {
    templatePresentation.text = executor.getStartActionText(configurationSettings.name)
    templatePresentation.icon = if (enabled) {
      executor.icon
    } else {
      toStrokeIcon(executor.disabledIcon, JBUI.CurrentTheme.RunWidget.RUN_ICON_COLOR)
    }
  }
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = enabled
  }
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  override fun actionPerformed(e: AnActionEvent) {
    ExecutionUtil.runConfiguration(configurationSettings, executor)
  }
}

