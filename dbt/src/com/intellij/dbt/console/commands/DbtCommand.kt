package com.intellij.dbt.console.commands

import com.intellij.dbt.run.DbtRunConfiguration
import com.intellij.dbt.run.DbtRunConfigurationFactory
import com.intellij.dbt.run.DbtRunConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

enum class DbtCommand(@Nls val commandName: String, val command: List<String> = emptyList()) {
  BUILD("build", listOf("build")),
  RUN("run", listOf("run")),
  DEBUG("debug", listOf("debug")),
  LIST("list", listOf("list")),
  TEST("test", listOf("test")),
  DOCS_GENERATE("docs", listOf("docs")),
  SHOW("show", listOf("show")),
  COMPILE("compile", listOf("compile")),
  CLONE("clone", listOf("clone")),
  DEPS("deps", listOf("deps"));

  override fun toString(): String = this.commandName

  fun getRunConfigurationTemplate(project: Project) : RunConfiguration {
    val runConfiguration = DbtRunConfiguration(project, DbtRunConfigurationFactory(DbtRunConfigurationType.getInstance()))
    runConfiguration.setDbtCommand(this)
    return runConfiguration
  }

  companion object {
    val getTopDbtCommands = setOf(BUILD, RUN, DEBUG, LIST, TEST)
  }
}