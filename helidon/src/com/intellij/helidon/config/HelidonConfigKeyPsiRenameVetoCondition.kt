// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement

internal class HelidonConfigKeyPsiRenameVetoCondition : Condition<PsiElement> {
  override fun value(psiElement: PsiElement): Boolean {
    return psiElement is HelidonConfigKeyDeclarationPsiElement
  }
}