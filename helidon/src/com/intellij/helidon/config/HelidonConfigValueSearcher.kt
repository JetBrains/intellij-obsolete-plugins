// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.CommonProcessors.FindFirstProcessor
import com.intellij.util.Processor

class HelidonConfigValueSearcher(
  private val module: Module, private val includeTests: Boolean = false,
  private val configKey: String, private val checkRelaxedNames: Boolean = true,
) {
  fun findValueText(): String? {
    val processor = object : FindFirstProcessor<HelidonConfigValueResult>() {
      override fun accept(result: HelidonConfigValueResult): Boolean {
        return result.valueText != null
      }
    }
    process(processor)
    return processor.foundValue?.valueText
  }

  fun process(processor: Processor<in HelidonConfigValueResult>): Boolean {
    val metaConfigKey = HelidonMetaConfigKeyManager.getInstance().findApplicationMetaConfigKey(module, configKey) ?: return true
    val configs = HelidonConfigFileContributor.findConfigFiles(module, includeTests)
    val psiManager = PsiManager.getInstance(module.project)
    for (config in configs) {
      val configPsiFile = psiManager.findFile(config.first) ?: continue
      val params = HelidonConfigValueSearchParams(module, configPsiFile, checkRelaxedNames, metaConfigKey)
      if (!config.second.processConfigValues(params, processor)) {
        return false
      }
    }
    return true
  }

  data class HelidonConfigValueSearchParams(
    val module: Module,
    val configFile: PsiFile,
    val checkRelaxedNames: Boolean = true,
    val configKey: MetaConfigKey,
  )

  data class HelidonConfigValueResult(
    val keyElement: PsiElement, val valueElement: PsiElement?,
    val valueText: String?,
    val params: HelidonConfigValueSearchParams,
  )
}