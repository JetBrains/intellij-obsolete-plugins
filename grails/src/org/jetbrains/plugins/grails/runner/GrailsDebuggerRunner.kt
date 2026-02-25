/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.runner

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.debugger.impl.RemoteConnectionBuilder
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.plugins.grails.runner.impl.GrailsCommandLineState
import org.jetbrains.plugins.grails.runner.impl.GrailsRunAppCommandLineState
import org.jetbrains.plugins.grails.runner.impl.GrailsTestAppCommandLineState
import org.jetbrains.plugins.grails.runner.impl.GrailsTestCommandLineState
import org.jetbrains.plugins.grails.structure.Grails3Application
import org.jetbrains.plugins.grails.structure.impl.Grails2Application
import org.jetbrains.plugins.grails.util.version.Version.GRAILS_3_1_5

class GrailsDebuggerRunner : GenericDebuggerRunner() {

  private val DEBUG_KEY_v2_SINGLE = "--debug"
  private val DEBUG_KEY_v2_FORKED = "--debug-fork"
  private val DEBUG_KEY_v3 = "--debug-jvm"
  private val POLL_TIMEOUT = 5 * 60 * 1000L // 5 minutes

  override fun getRunnerId(): String = javaClass.name

  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == DefaultDebugExecutor.EXECUTOR_ID &&
    profile is GrailsRunConfiguration &&
    profile.grailsApplicationNullable.let { it is Grails3Application || it is Grails2Application }

  override fun createContentDescriptor(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    if (state !is GrailsCommandLineState || state !is GrailsRunAppCommandLineState && state !is GrailsTestAppCommandLineState) {
      return super.createContentDescriptor(state, environment)
    }

    val connection: RemoteConnection = when (val application = state.application) {
      is Grails2Application -> if (isForkedDebug(state)) {
        state.command.args.removeIf { it == DEBUG_KEY_v2_FORKED }
        val javaParams = state.javaParameters
        val (connection, vmOptions) = createOptionsAndConnection(javaParams, false, environment.project)
        val propertyName = if (state is GrailsTestCommandLineState) {
          "grails.project.fork.test.debugArgs"
        }
        else {
          "grails.project.fork.run.debugArgs"
        }
        javaParams.vmParametersList.addProperty(propertyName, vmOptions)
        connection
      }
      else {
        state.command.args.removeIf { it == DEBUG_KEY_v2_SINGLE }
        val javaParams = state.javaParameters
        val (connection, vmOptions) = createOptionsAndConnection(javaParams, true, environment.project)
        javaParams.vmParametersList.addParametersString(vmOptions)
        connection
      }
      is Grails3Application -> if (application.grailsVersion >= GRAILS_3_1_5 && !Registry.`is`("grails.simple.debug")) {
        state.command.args.removeIf { it == DEBUG_KEY_v3 }
        val javaParams = state.javaParameters
        val (connection, vmOptions) = createOptionsAndConnection(javaParams, true, environment.project)
        val existing = javaParams.env[OPTS_KEY]
        javaParams.env[OPTS_KEY] = if (existing.isNullOrBlank()) vmOptions else "$existing $vmOptions"
        connection
      }
      else {
        state.command.args.apply { if (!contains(DEBUG_KEY_v3)) add(DEBUG_KEY_v3) }
        RemoteConnection(true, "localhost", "5005", false)
      }
      else -> return null
    }

    return attachVirtualMachine(state, environment, connection, POLL_TIMEOUT)
  }

  private fun createOptionsAndConnection(javaParams: JavaParameters, asyncDebugger: Boolean, project: Project): Pair<RemoteConnection, String> {
    val javaParamsDelegate = object : JavaParameters() {
      override fun getClassPath() = javaParams.classPath
    }
    javaParamsDelegate.jdk = javaParams.jdk
    val connection = RemoteConnectionBuilder(false, DebuggerSettings.SOCKET_TRANSPORT, null)
      .suspend(false)
      .asyncAgent(asyncDebugger)
      .project(project)
      .create(javaParamsDelegate)
    val vmOptions = javaParamsDelegate.vmParametersList.parametersString
    return Pair(connection, vmOptions)
  }

}