package com.intellij.dbt.run

import com.intellij.dbt.run.producer.DbtBaseRunConfigurationProducer
import com.intellij.dbt.run.producer.DbtRunRunConfigurationProducer
import com.intellij.dbt.run.producer.DbtShowRunConfigurationProducer
import com.intellij.dbt.run.producer.DbtTestRunConfigurationProducer
import com.intellij.execution.ExecutorRegistryImpl.RunnerHelper
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder.Companion.createOrNull
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction

abstract class DbtBaseRunModelAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val file = e.getData(CommonDataKeys.PSI_FILE)
    if (file == null) return
    val virtualFile = file.virtualFile
    if (virtualFile == null) return

    val project = file.project
    val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
    val configProducer = getRunConfigurationProducer()
    var configurationSettings: RunnerAndConfigurationSettings? = configProducer.findExistingConfiguration(context)
    val runConfiguration: DbtRunConfiguration
    if (configurationSettings == null) {
      val configurationFromContext = configProducer.createConfigurationFromContext(context) ?: return
      configurationSettings = configurationFromContext.configurationSettings
      runConfiguration = configurationFromContext.configuration as? DbtRunConfiguration ?: return
      val runManager = RunManager.getInstance(project)
      runManager.addConfiguration(configurationSettings)
      runManager.selectedConfiguration = configurationFromContext.configurationSettings
    }
    else {
      runConfiguration = configurationSettings.configuration as? DbtRunConfiguration ?: return
    }

    val builder = createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration)
    if (builder != null) {
      val executor = DefaultRunExecutor.getRunExecutorInstance()
      RunnerHelper.run(project, runConfiguration, configurationSettings, e.dataContext, executor)
    }
  }

  abstract fun getRunConfigurationProducer(): DbtBaseRunConfigurationProducer
}

class DbtRunModelAction : DbtBaseRunModelAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.icon = com.intellij.icons.AllIcons.RunConfigurations.TestState.Run
  }

  override fun getRunConfigurationProducer() = RunConfigurationProducer.getInstance(DbtRunRunConfigurationProducer::class.java)
}

class DbtTestModelAction : DbtBaseRunModelAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.icon = com.intellij.icons.AllIcons.Scope.Tests
  }

  override fun getRunConfigurationProducer() = RunConfigurationProducer.getInstance(DbtTestRunConfigurationProducer::class.java)
}

class DbtPreviewModelAction : DbtBaseRunModelAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.icon = com.intellij.icons.AllIcons.Actions.Preview
  }

  override fun getRunConfigurationProducer() = RunConfigurationProducer.getInstance(DbtShowRunConfigurationProducer::class.java)
}