/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.runner.impl

import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil.getSplitterPropertyName
import com.intellij.execution.testframework.sm.runner.GeneralToSMTRunnerEventsConvertor
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerUIActionsHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.gradle.execution.test.runner.GradleConsoleProperties
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole
import org.jetbrains.plugins.gradle.execution.test.runner.events.GradleTestsExecutionConsoleOutputProcessor
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration
import org.jetbrains.plugins.grails.runner.pathToGradleInitScript
import org.jetbrains.plugins.grails.runner.pathToGradleInitScriptKey
import org.jetbrains.plugins.grails.runner.testFrameworkName
import org.jetbrains.plugins.groovy.mvc.MvcCommand

class Grails3TestCommandLineState(
  environment: ExecutionEnvironment,
  configuration: GrailsRunConfiguration,
  executor: GrailsCommandLineExecutor
) : GrailsTestAppCommandLineState(environment, configuration, executor) {

  private val myCommand by lazy {
    val originalCommand = super.getCommand()
    MvcCommand("intellij-command-proxy").apply {
      args += originalCommand.command
      args += originalCommand.args
      env = originalCommand.env
      vmOptions = originalCommand.vmOptions
      envVariables = originalCommand.envVariables
      isPassParentEnvs = originalCommand.isPassParentEnvs
      envVariables[pathToGradleInitScriptKey] = pathToGradleInitScript!!
    }
  }

  override fun getCommand(): MvcCommand = myCommand

  override fun createConsole(executor: Executor): ConsoleView {
    val splitterPropertyName = getSplitterPropertyName(testFrameworkName)
    val consoleProperties = GradleConsoleProperties(configuration, testFrameworkName, executor)
    val console = GradleTestsExecutionConsole(consoleProperties, splitterPropertyName)
    console.setHelpId("reference.runToolWindow.testResultsTab")
    console.initUI()
    console.addAttachToProcessListener { handler ->
      val resultsViewer = console.resultsViewer
      resultsViewer.addEventsListener(SMTRunnerUIActionsHandler(consoleProperties))

      val rootNode = resultsViewer.testsRootNode
      rootNode.handler = handler

      val eventsProcessor = GeneralToSMTRunnerEventsConvertor(consoleProperties.project, rootNode, testFrameworkName)
      eventsProcessor.addEventsListener(resultsViewer)
      eventsProcessor.onStartTesting()

      handler.addProcessListener(object : ProcessAdapter() {
        override fun processTerminated(event: ProcessEvent) {
          eventsProcessor.onFinishTesting()
        }

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
          GradleTestsExecutionConsoleOutputProcessor.onOutput(console, event.text, outputType)
        }
      })
    }
    return console
  }
}
