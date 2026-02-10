// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

internal class HelidonYamlKeyReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val yamlKeyValue = element as YAMLKeyValue
    if (yamlKeyValue.key == null) return PsiReference.EMPTY_ARRAY

    return arrayOf(HelidonYamlKeyMetaConfigKeyReference(yamlKeyValue))
  }
}