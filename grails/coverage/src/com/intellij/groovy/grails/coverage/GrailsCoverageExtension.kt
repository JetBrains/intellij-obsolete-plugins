// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.coverage

import com.intellij.coverage.JavaCoverageEngineExtension
import com.intellij.execution.configurations.RunConfigurationBase
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration
import org.jetbrains.plugins.grails.runner.canPassVmArgs

internal class GrailsCoverageExtension : JavaCoverageEngineExtension() {
  override fun isApplicableTo(conf: RunConfigurationBase<*>?): Boolean {
    return conf is GrailsRunConfiguration && conf.grailsApplicationNullable.canPassVmArgs()
  }
}