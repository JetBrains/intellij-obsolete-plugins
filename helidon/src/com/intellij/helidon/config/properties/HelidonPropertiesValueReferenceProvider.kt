// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.createHelidonPlaceholderReferences
import com.intellij.helidon.config.hints.HelidonHintReferencesProvider
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext

internal class HelidonPropertiesValueReferenceProvider : PsiReferenceProvider() {

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val property = PsiTreeUtil.getParentOfType(element, PropertyImpl::class.java) ?: return PsiReference.EMPTY_ARRAY
    val propertyKey = HelidonPropertiesUtils.getPropertyKey(property) ?: return PsiReference.EMPTY_ARRAY
    val placeholderReferences = createHelidonPlaceholderReferences(element)
    val key = MetaConfigKeyReference.getResolvedMetaConfigKey(propertyKey) ?: return placeholderReferences
    val valueTextRanges: List<TextRange> =
      if (canHaveMultipleValues(propertyKey, key)) {
        MicroservicesConfigUtils.getListValueRanges(element)
      }
      else {
        listOf(ElementManipulators.getValueTextRange(element))
      }

    val providerReferences = HelidonHintReferencesProvider.getInstance().getValueReferences(key, propertyKey, element, valueTextRanges, context)
    return ArrayUtil.mergeArrays(providerReferences, placeholderReferences)
  }

  private fun canHaveMultipleValues(propertyKey: PropertyKeyImpl, key: MetaConfigKey): Boolean {
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return false
    }
    if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      return !isUsingIndexedAccess(propertyKey) // indexed access -> single value
    }
    if (isUsingIndexedAccess(propertyKey)) {
      return false
    }
    val mapValueType = key.effectiveValueType ?: return false
    val mapValueAccessType = MetaConfigKey.AccessType.forPsiType(mapValueType)
    return mapValueAccessType === MetaConfigKey.AccessType.INDEXED
  }

  private fun isUsingIndexedAccess(propertyKey: PropertyKeyImpl): Boolean {
    return propertyKey.textContains('[')
  }
}