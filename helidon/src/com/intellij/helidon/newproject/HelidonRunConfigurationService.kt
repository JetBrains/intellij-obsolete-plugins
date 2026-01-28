// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.newproject

import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.helidon.constants.HelidonConstants
import com.intellij.helidon.utils.HelidonCommonUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable
import java.util.function.Consumer

@Service(Service.Level.PROJECT)
class HelidonRunConfigurationService {
  fun createRunConfigurations(project: Project) {
    val application = ApplicationManager.getApplication()
    if (application.isHeadlessEnvironment || application.isUnitTestMode) {
      return
    }

    if (!isNewProject(project)) {
      // so far only newly created projects supported
      return
    }

    ReadAction.nonBlocking(Callable {
      ModuleManager.getInstance(project).modules.asSequence()
        .filter { !it.name.endsWith(".test") }
        .filter { HelidonCommonUtils.hasHelidonMPLibrary(it) }
        .filter {
          RunManager.getInstance(it.project)
            .getConfigurationSettingsList(ApplicationConfigurationType::class.java).isEmpty()
        }
        .toList()
    })
      .coalesceBy(this)
      .inSmartMode(project)
      .finishOnUiThread(ModalityState.nonModal(), Consumer { modules ->
        for (module in modules) {
          if (module.isDisposed) continue

          runWriteAction {
            createMicroProfileRunConfiguration(module)
          }
        }
      })
      .submit(AppExecutorUtil.getAppExecutorService())
  }

  private fun isNewProject(project: Project): Boolean {
    return project.getUserData(NEW_HELIDON_PROJECT_KEY) == java.lang.Boolean.TRUE
  }

  private fun createMicroProfileRunConfiguration(module: Module) {
    val runManager = RunManager.getInstance(module.project)
    try {
      val settings = runManager.createConfiguration("", ApplicationConfigurationType.getInstance().configurationFactories[0])
      val newRunConfig = settings.configuration as ApplicationConfiguration
      newRunConfig.setModule(module)
      newRunConfig.mainClassName = HelidonConstants.MP_MAIN
      settings.name = module.name
      newRunConfig.setGeneratedName()
      runManager.setUniqueNameIfNeeded(settings)
      runManager.addConfiguration(settings)
      if (runManager.allSettings.size == 1) {
        runManager.selectedConfiguration = settings
      }
    }
    catch (e: ProcessCanceledException) {
      //reattempt to create run configuration
      throw e
    }
    catch (t: Throwable) {
      logger<HelidonRunConfigurationService>().error("Error creating Helidon run configuration for module ${module.name}", t)
    }
  }
}