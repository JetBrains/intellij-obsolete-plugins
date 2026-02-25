/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.runner

import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ParametersList
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.plugins.gradle.service.execution.createTestInitScript
import org.jetbrains.plugins.grails.runner.impl.GrailsCommandLineState
import org.jetbrains.plugins.grails.runner.impl.GrailsTestCommandLineState
import org.jetbrains.plugins.grails.structure.Grails3Application
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.impl.Grails2Application
import org.jetbrains.plugins.grails.util.version.Version.GRAILS_3_1_5

internal const val OPTS_KEY = "GRAILS_FORK_OPTS"
private const val plainOutputKeyV3 = "--plain-output"

internal const val testFrameworkName = "GrailsTests"
internal const val pathToGradleInitScriptKey = "INTELLIJ_GRADLE_INIT_SCRIPT"
internal val pathToGradleInitScript: String by lazy {
  createTestInitScript().toString()
}

fun GrailsApplication?.canPassVmArgs(): Boolean = this is Grails2Application || this is Grails3Application && this.grailsVersion >= GRAILS_3_1_5

fun setupJavaParameters(configuration: GrailsRunConfiguration, state: GrailsCommandLineState, params: JavaParameters) {
  val delegate = JavaParameters().apply {
    jdk = params.jdk
    classPath.addAll(params.classPath.pathList)
  }
  JavaRunConfigurationExtensionManager.instance.updateJavaParameters(configuration, delegate, state.runnerSettings,
                                                                     state.environment.executor)
  val vmOptions = delegate.vmParametersList.parametersString

  when (val application = configuration.grailsApplication) {
    is Grails2Application -> if (isForkedDebug(state)) {
      val propertyName = if (state is GrailsTestCommandLineState) {
        "grails.project.fork.test.debugArgs"
      }
      else {
        "grails.project.fork.run.debugArgs"
      }
      params.vmParametersList.addProperty(propertyName, vmOptions)
    }
    else {
      params.vmParametersList.addParametersString(vmOptions)
    }
    is Grails3Application -> if (application.grailsVersion >= GRAILS_3_1_5) {
      val existing = params.env[OPTS_KEY]
      params.env[OPTS_KEY] = if (existing.isNullOrBlank()) vmOptions else "$existing $vmOptions"
    }
  }
}

internal fun isForkedDebug(state: GrailsCommandLineState): Boolean {
  val application = state.application
  if (application !is Grails2Application) return false
  val version = application.grailsVersion
  val forTests = state is GrailsTestCommandLineState
  // There is a bug (https://github.com/grails/grails-core/issues/5641) in grails < 2.3.10
  // that doesn't allow us to debug "test-app" in forked mode.
  // We should have ability to debug "run-*" commands in forked mode starting from 2.3.0.
  //
  // But there is another bug (https://github.com/grails/grails-core/issues/3294)
  // that makes no sense to disable forked mode only for tests,
  // i.e. forked mode must be disabled for "run-*" too.
  //
  // Finally we will allow debug tests for >=2.3.10 and debug "run-*" for >=2.3.5,
  // and force users to disable forked mode in older versions.
  return if (forTests)
    version >= org.jetbrains.plugins.grails.util.version.Version.GRAILS_2_3_10 && application.isTestForked
  else {
    version >= org.jetbrains.plugins.grails.util.version.Version.GRAILS_2_3_5 && application.isRunForked
  }
}

internal fun addPlainOutput(parameters: ParametersList) {
  if (Registry.`is`("grails.add.plain.output") && !parameters.hasParameter(plainOutputKeyV3)) {
    parameters.add(plainOutputKeyV3)
  }
}
