package com.intellij.dbt.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface DbtCommandLinePatcher {
  fun patchCommandLine(cmd: GeneralCommandLine, project: Project): GeneralCommandLine

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<DbtCommandLinePatcher> = ExtensionPointName.create("com.intellij.dbt.commandLinePatcher")
  }
}