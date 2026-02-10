// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.helidon.config.HelidonConfigFileContributor
import com.intellij.helidon.config.HelidonConfigValueSearcher.HelidonConfigValueResult
import com.intellij.helidon.config.HelidonConfigValueSearcher.HelidonConfigValueSearchParams
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.references.PropertiesCompletionContributor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Processor

internal class HelidonPropertiesConfigFileContributor : HelidonConfigFileContributor(PropertiesFileType.INSTANCE) {

  override fun findKey(psiFile: PsiFile, key: String): PsiElement? {
    return (psiFile as? PropertiesFile)?.findPropertyByKey(key)?.psiElement
  }

  override fun getKeyVariants(psiFile: PsiFile): Collection<LookupElement> {
    return (psiFile as? PropertiesFile)?.properties?.mapNotNull(PropertiesCompletionContributor::createVariant) ?: return emptyList()
  }

  override fun processConfigValues(params: HelidonConfigValueSearchParams,
                                   processor: Processor<in HelidonConfigValueResult>): Boolean {
    val propertiesFile = params.configFile as? PropertiesFile ?: return true
    val properties = propertiesFile.properties.asReversed()
    if (properties.isEmpty()) return true

    val keyName = params.configKey.name
    val binder = HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(params.module)

    for (property in properties) {
      ProgressManager.checkCanceled()
      val propertyName = property.name ?: continue

      if (propertyName == keyName ||
          (params.checkRelaxedNames && binder.bindsTo(params.configKey, propertyName))) {
        if (!processor.process(createResult(property, params))) {
          return false
        }
      }
    }
    return true
  }

  private fun createResult(property: IProperty, params: HelidonConfigValueSearchParams): HelidonConfigValueResult {
    val propertyImpl = property.psiElement as PropertyImpl
    val key = HelidonPropertiesUtils.getPropertyKey(propertyImpl)!!
    return HelidonConfigValueResult(key, HelidonPropertiesUtils.getPropertyValue(propertyImpl), propertyImpl.value, params)
  }
}