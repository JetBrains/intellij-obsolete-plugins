package com.intellij.dbt.console

import com.intellij.dbt.DbtUtils.Companion.getDbtSettings
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.run.DbtCommandLinePatcher
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.module.Module

fun getDbtExecutableName() = if (com.intellij.openapi.util.SystemInfo.isWindows) "dbt.exe" else "dbt"

fun getDbtCommandLine(command: DbtCommand, module: Module, workingDirectory: String?): GeneralCommandLine {
  val dbtExecutableFile = getDbtSettings(module)?.dbtExecutablePath ?: getDbtExecutableName()
  var result = GeneralCommandLine(listOf(dbtExecutableFile) + command.command).withWorkDirectory(workingDirectory)
  DbtCommandLinePatcher.EP_NAME.extensionList.forEach {
    result = it.patchCommandLine(result, module.project)
  }

  return result
}