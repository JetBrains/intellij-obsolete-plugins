package com.intellij.dbt.run

import com.intellij.dbt.DbtUtils.Companion.getDbtSettings
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.console.getDbtCommandLine
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.module.Module
import com.intellij.platform.backend.workspace.virtualFile

class DbtRunConfigurationOptions : ModuleBasedConfigurationOptions() {
  var dbtCommand by enum<DbtCommand>()
  var dbtAdditionalArguments by list<String>()
  var envVars by map<String, String>()

  fun getCommandLine(module: Module) : GeneralCommandLine {
    var workingDirectory = getDbtSettings(module)?.dbtProjectPath?.virtualFile?.path

    if (workingDirectory == null) {
      workingDirectory = module.project.basePath
    }
    return getDbtCommandLine(getCommand(), module, workingDirectory)
      .also { commandLine ->
        dbtAdditionalArguments.let { commandLine.withParameters(it) }
        commandLine.withEnvironment(envVars)
      }
  }

  fun getCommand() = dbtCommand ?: DbtCommand.RUN
}