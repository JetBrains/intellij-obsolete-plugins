/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.runner.impl

import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration

abstract class GrailsTestAppCommandLineState(
  environment: ExecutionEnvironment,
  configuration: GrailsRunConfiguration,
  executor: GrailsCommandLineExecutor
) : GrailsCommandLineState(environment, configuration, executor)
