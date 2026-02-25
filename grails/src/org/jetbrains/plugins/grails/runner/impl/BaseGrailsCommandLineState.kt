/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.runner.impl

import com.intellij.execution.ExecutionException
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration

abstract class BaseGrailsCommandLineState(environment: ExecutionEnvironment, val configuration: GrailsRunConfiguration) : JavaCommandLineState(environment) {
  /**
   * @see com.intellij.execution.application.BaseJavaApplicationCommandLineState.startProcess()
   */
  @Throws(ExecutionException::class)
  override fun startProcess(): OSProcessHandler = KillableColoredProcessHandler(createCommandLine()).apply {
    ProcessTerminatedListener.attach(this)
    JavaRunConfigurationExtensionManager.instance.attachExtensionsToProcess(configuration, this, runnerSettings)
  }
}