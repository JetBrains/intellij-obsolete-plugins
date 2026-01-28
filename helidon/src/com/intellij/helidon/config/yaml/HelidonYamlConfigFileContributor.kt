// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.helidon.config.HelidonConfigFileContributor
import com.intellij.helidon.config.HelidonConfigValueSearcher.HelidonConfigValueResult
import com.intellij.helidon.config.HelidonConfigValueSearcher.HelidonConfigValueSearchParams
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.yaml.ConfigYamlAccessor
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Processor
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.*

internal class HelidonYamlConfigFileContributor : HelidonConfigFileContributor(YAMLFileType.YML) {
  override fun findKey(psiFile: PsiFile, key: String): PsiElement? {
    if (psiFile !is YAMLFile) return null

    for (document in psiFile.documents) {
      val existingKey = ConfigYamlAccessor(document, HelidonMetaConfigKeyManager.getInstance()).findExistingKey(key)
      if (existingKey != null) return existingKey
    }
    return null
  }

  override fun getKeyVariants(psiFile: PsiFile): Collection<LookupElement> {
    if (psiFile !is YAMLFile) return emptyList()

    val result = ArrayList<LookupElement>()
    for (document in psiFile.documents) {
      val accessor = ConfigYamlAccessor(document, HelidonMetaConfigKeyManager.getInstance())
      for (yamlKeyValue in accessor.allKeys) {
        val yamlValue = yamlKeyValue.value
        if (yamlValue !is YAMLScalar &&
            yamlValue !is YAMLSequence) {
          continue
        }
        val qualifiedKey = ConfigYamlUtils.getQualifiedConfigKeyName(yamlKeyValue)
        result.add(LookupElementBuilder.create(yamlKeyValue, qualifiedKey).withRenderer(ConfigYamlUtils.getYamlPlaceholderLookupRenderer()))
      }
    }
    return result
  }

  override fun processConfigValues(params: HelidonConfigValueSearchParams, processor: Processor<in HelidonConfigValueResult>): Boolean {
    val yamlFile = params.configFile as? YAMLFile ?: return true
    val keyName = params.configKey.name
    val binder = HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(params.module)

    val keyValueProcessor = Processor { yamlKeyValue: YAMLKeyValue ->
      ProgressManager.checkCanceled()
      val qualifiedKeyName = ConfigYamlUtils.getQualifiedConfigKeyName(yamlKeyValue)
      if (qualifiedKeyName == keyName ||
          (params.checkRelaxedNames && binder.bindsTo(params.configKey, qualifiedKeyName))) {
        val keyElement = yamlKeyValue.key ?: return@Processor true
        val valueElement = yamlKeyValue.value
        if (valueElement is YAMLSequence || valueElement is YAMLMapping) return@Processor true
        val valueText = yamlKeyValue.valueText
        val sanitizedValueText = ConfigYamlUtils.sanitizeNumberValueIfNeeded(valueText) { params.configKey.effectiveValueElementType } ?: valueText
        return@Processor processor.process(HelidonConfigValueResult(keyElement, valueElement, sanitizedValueText, params))
      }
      true
    }

    for (document in yamlFile.documents.asReversed()) {
      if (!ConfigYamlAccessor.processAllKeysReversed(document, keyValueProcessor)) {
        return false
      }
    }
    return true
  }
}