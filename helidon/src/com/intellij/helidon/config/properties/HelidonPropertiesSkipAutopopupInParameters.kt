// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.helidon.config.HelidonParametrizedConfigKey
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

internal class HelidonPropertiesSkipAutopopupInParameters : CompletionConfidence() {
  override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
    if (contextElement !is PropertyKeyImpl ||
        !hasHelidonLibrary(contextElement.project) ||
        !isHelidonConfigFile(psiFile)) {
      return ThreeState.UNSURE
    }

    val configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(contextElement) ?: return ThreeState.UNSURE
    val parametrizedConfigKey = HelidonParametrizedConfigKey.getParametrizedConfigKey(configKey.name) ?: return ThreeState.UNSURE
    val offsetInElement = offset - contextElement.startOffset
    val parameterRange = parametrizedConfigKey.getParameterRange(contextElement.text)?.grown(1) ?: return ThreeState.UNSURE
    return if (parameterRange.contains(offsetInElement)) ThreeState.YES else ThreeState.UNSURE
  }
}