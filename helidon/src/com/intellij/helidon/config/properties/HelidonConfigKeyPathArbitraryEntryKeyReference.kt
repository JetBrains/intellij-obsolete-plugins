// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.microservices.jvm.config.ConfigKeyPathContext
import com.intellij.microservices.jvm.config.ConfigKeyPathReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiType

internal class HelidonConfigKeyPathArbitraryEntryKeyReference(element: PsiElement, textRange: TextRange) :
  PsiReferenceBase<PsiElement>(element, textRange), ConfigKeyPathReference {
  override fun resolve(): PsiElement? = null

  override fun getPathType(): ConfigKeyPathReference.PathType = ConfigKeyPathReference.PathType.ARBITRARY_ENTRY_KEY

  override fun isSoft(): Boolean = true

  override fun getKeyClass(): PsiClass? = null

  override fun getValueType(): PsiType? = null

  override fun getValueElementType(): PsiType? = null

  override fun getContext(): ConfigKeyPathContext {
    throw UnsupportedOperationException("Context cannot be provided")
  }
}