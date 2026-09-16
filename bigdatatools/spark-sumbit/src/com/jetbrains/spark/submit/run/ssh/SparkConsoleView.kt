package com.jetbrains.spark.submit.run.ssh

import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.impl.ConsoleViewUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SparkConsoleView(
  project: Project,
  val searchScope: GlobalSearchScope = GlobalSearchScope.allScope(project),
  val excludeFilters: (Filter) -> Boolean = { true }
) : ConsoleViewImpl(project, searchScope, true, false) {
  init {
    SafeExecutor.createInstance(this).coroutineScope.launch {
      val filters = readAction {
        ConsoleViewUtil.computeConsoleFilters(project, this@SparkConsoleView, searchScope)
      }
      withContext(Dispatchers.EDT) {
        for (filter in filters) {
          if (!excludeFilters(filter)) {
            addMessageFilter(filter)
          }
        }
      }
    }
  }
}