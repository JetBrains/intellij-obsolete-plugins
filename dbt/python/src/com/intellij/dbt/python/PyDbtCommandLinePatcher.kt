package com.intellij.dbt.python

import com.intellij.dbt.run.DbtCommandLinePatcher
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.sdk.PySdkUtil

internal class PyDbtCommandLinePatcher: DbtCommandLinePatcher {
  override fun patchCommandLine(cmd: GeneralCommandLine, project: Project): GeneralCommandLine {
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    if (sdk != null) {
      cmd.withEnvironment(PySdkUtil.activateVirtualEnv(sdk))
    }
    return cmd
  }
}