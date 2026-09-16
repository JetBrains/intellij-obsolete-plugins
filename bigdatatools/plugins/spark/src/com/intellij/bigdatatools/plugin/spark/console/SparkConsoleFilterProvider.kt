package com.intellij.bigdatatools.plugin.spark.console

import com.intellij.execution.filters.ConsoleDependentInputFilterProvider
import com.intellij.execution.filters.InputFilter
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.spark.submit.run.ssh.SparkConsoleView

class SparkConsoleFilterProvider : ConsoleDependentInputFilterProvider() {
  override fun getDefaultFilters(consoleView: ConsoleView, project: Project, scope: GlobalSearchScope): List<InputFilter> {
    if (consoleView is SparkConsoleView)
      return listOf(SparkConsoleInputFilter())
    else
      return emptyList()


  }

}