package com.intellij.dbt.run

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project


open class DbtRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun getId(): String = DbtRunConfigurationType.ID

  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return DbtRunConfiguration(project, this)
  }

  override fun getOptionsClass(): Class<out BaseState>? {
    return DbtRunConfigurationOptions::class.java
  }

  companion object {
    fun addTopCommandDbtRunConfigurations(module: Module) {
      val project = module.project
      val dbtConfigurationFactory = DbtRunConfigurationFactory(DbtRunConfigurationType.getInstance())
      DbtCommand.getTopDbtCommands.forEach {
        val templateConfiguration = dbtConfigurationFactory.createConfiguration(
          DbtBundle.message("action.open.DbtConsole.text", it.commandName), it.getRunConfigurationTemplate(project)
        )
        val configuration = RunManager.getInstance(project).createConfiguration(templateConfiguration, dbtConfigurationFactory)
        (configuration.configuration as DbtRunConfiguration).setModule(module)
        RunManager.getInstance(project).addConfiguration(configuration)
        if (it == DbtCommand.BUILD) {
          RunManager.getInstance(project).selectedConfiguration = configuration
        }
      }
    }
  }
}