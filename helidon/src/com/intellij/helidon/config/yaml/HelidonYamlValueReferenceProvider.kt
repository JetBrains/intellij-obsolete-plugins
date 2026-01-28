// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.createHelidonPlaceholderReferences
import com.intellij.helidon.config.hints.HelidonHintReferencesProvider
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils
import com.intellij.microservices.jvm.config.hints.NumberHintReferenceBase
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLValue

internal class HelidonYamlValueReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java) ?: return PsiReference.EMPTY_ARRAY
    val yamlScalar = element as YAMLScalar
    if (yamlScalar.isMultiline) PsiReference.EMPTY_ARRAY

    val placeholderReferences = createHelidonPlaceholderReferences(element)
    val key = MetaConfigKeyReference.getResolvedMetaConfigKey(yamlKeyValue) ?: return placeholderReferences
    val valueTextRanges: List<TextRange> =
      if (canHaveMultipleValues(yamlKeyValue.value, key)) {
        MicroservicesConfigUtils.getListValueRanges(element)
      }
      else {
        listOf(ElementManipulators.getValueTextRange(element))
      }
    context.put(NumberHintReferenceBase.NUMBER_VALUE_SANITIZER_KEY, ConfigYamlUtils.getYamlNumberValueSanitizer())
    val providerReferences = HelidonHintReferencesProvider.getInstance().getValueReferences(key, null, element, valueTextRanges, context)
    return ArrayUtil.mergeArrays(providerReferences, placeholderReferences)
  }

  private fun canHaveMultipleValues(valueElement: YAMLValue?, key: MetaConfigKey): Boolean {
    if (valueElement is YAMLSequence) return false

    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) return false

    if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) return true

    val mapValueType = key.effectiveValueType ?: return false
    val mapValueAccessType = MetaConfigKey.AccessType.forPsiType(mapValueType)
    return mapValueAccessType === MetaConfigKey.AccessType.INDEXED
  }
}