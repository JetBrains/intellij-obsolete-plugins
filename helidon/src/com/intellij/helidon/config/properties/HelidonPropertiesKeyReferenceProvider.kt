// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

internal class HelidonPropertiesKeyReferenceProvider : PsiReferenceProvider() {

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val property = PsiTreeUtil.getParentOfType(element, PropertyImpl::class.java) ?: return PsiReference.EMPTY_ARRAY
    return arrayOf(HelidonPropertyKeyMetaConfigKeyReference(element, property))
  }
}