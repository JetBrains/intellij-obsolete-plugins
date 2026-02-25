/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.runner.impl

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration
import org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils

class GrailsRunAppCommandLineState(
  environment: ExecutionEnvironment,
  configuration: GrailsRunConfiguration,
  executor: GrailsCommandLineExecutor
) : GrailsCommandLineState(environment, configuration, executor) {

  @Throws(ExecutionException::class)
  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val result = super.execute(executor, runner)
    val handler = result.processHandler
    if (handler != null && configuration.isLaunchBrowser) {
      handler.addProcessListener(GrailsExecutionUtils.getBrowserLaunchListener(handler, configuration.launchBrowserUrl))
    }
    return result
  }
}
