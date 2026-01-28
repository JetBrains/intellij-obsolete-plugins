// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns.psiClass
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.literalExpression
import com.intellij.psi.*
import org.jetbrains.uast.ULiteralExpression

internal class HelidonConfigPropertyReferenceContributor : PsiReferenceContributor() {

  private val configGetCallArgumentPattern: ElementPattern<ULiteralExpression> =
    literalExpression().inCall(
      callExpression()
        .withMethodName(HELIDON_CONFIG_GET_METHOD)
        .withReceiver(psiClass().withQualifiedName(HELIDON_CONFIG_FQN)))

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerUastReferenceProvider(
      configGetCallArgumentPattern,
      uastReferenceProvider { literal: ULiteralExpression, psiElement: PsiElement ->
        if (literal.isString)
          arrayOf(HelidonConfigPlaceholderReference.Builder(psiElement,
                                                            TextRange.from(1, psiElement.textLength - 2),
                                                            false)
                    .build())
        else
          PsiReference.EMPTY_ARRAY
      })
  }
}