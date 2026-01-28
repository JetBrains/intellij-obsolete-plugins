// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue

private fun isKeyDefinition(psiElement: PsiElement): Boolean = psiElement is LeafPsiElement && psiElement.getParent() is YAMLKeyValue

private fun isKeyReference(psiElement: PsiElement): Boolean = psiElement is YAMLKeyValue

internal class HelidonYamlKeyRenameVetoCondition : Condition<PsiElement> {
  override fun value(psiElement: PsiElement): Boolean {
    return (isKeyDefinition(psiElement) || isKeyReference(psiElement)) &&
           isInsideApplicationYamlFile(psiElement)
  }
}