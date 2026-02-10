// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.HelidonMetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.toArray

internal class HelidonConfigKeyPathReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference?> {
    val configKey = context[MetaConfigKeyReference.META_CONFIG_KEY] as HelidonMetaConfigKey
    if (configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) return PsiReference.EMPTY_ARRAY
    val elementText = element.text
    val set = HelidonConfigKeyPathReferenceSet(element, configKey, elementText, 0)
    return set.references.toArray(PsiReference.EMPTY_ARRAY)
  }
}