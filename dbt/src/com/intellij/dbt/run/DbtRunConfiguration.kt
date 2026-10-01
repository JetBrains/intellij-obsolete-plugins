package com.intellij.dbt.run

import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.fus.DbtRunConfigurationCounterCollector
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfigurationModule
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

val DBT_COMMAND_ARGUMENT_SELECT = "--select"
val DBT_COMMAND_ARGUMENT_SELECT_SHORT = "--s"
val DBT_COMMAND_ARGUMENT_SELECT_VARIANTS = listOf(DBT_COMMAND_ARGUMENT_SELECT, DBT_COMMAND_ARGUMENT_SELECT_SHORT)

class DbtRunConfiguration(project: Project, configurationFactory: ConfigurationFactory) :
  ModuleBasedConfiguration<RunConfigurationModule, org.jdom.Element>(RunConfigurationModule(project), configurationFactory) {

  override fun getOptions() = super.getOptions() as DbtRunConfigurationOptions

  fun getDbtOption(): DbtRunConfigurationOptions = options

  fun setDbtCommand(dbtCommand: DbtCommand) {
    options.dbtCommand = dbtCommand
  }

  fun setDbtArguments(dbtAdditionalArguments : List<String> = emptyList()) {
    options.dbtAdditionalArguments = dbtAdditionalArguments.toMutableList()
  }

  fun getDbtCommand() = options.dbtCommand

  fun isForFile(file: VirtualFile): Boolean {
    for (i in 0..<options.dbtAdditionalArguments.size-1) {
      if (options.dbtAdditionalArguments[i] in DBT_COMMAND_ARGUMENT_SELECT_VARIANTS) {
        return file.path.endsWith(options.dbtAdditionalArguments[i + 1])
      }
    }
    return false
  }

  override fun getState(executor: Executor,
                        environment: ExecutionEnvironment): RunProfileState? {
    val module = configurationModule.module ?: return null
    return object : CommandLineState(environment) {

      override fun startProcess(): ProcessHandler {
        DbtRunConfigurationCounterCollector.logRunEvent(getDbtOption().getCommand())
        val envVars = getDbtOption().envVars

        val commandLine = getDbtOption().getCommandLine(module).withEnvironment(envVars)
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
      }
    }
  }

  override fun getConfigurationEditor() = DbtRunConfigurationEditor()

  override fun getValidModules(): MutableCollection<Module> {
    return mutableListOf()
  }
}