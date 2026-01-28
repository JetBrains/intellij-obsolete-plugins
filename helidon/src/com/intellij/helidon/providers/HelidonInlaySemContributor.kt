// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers

import com.intellij.helidon.utils.HelidonCommonUtils
import com.intellij.microservices.jvm.url.uastUrlPathReferenceInjectorForScheme
import com.intellij.microservices.jvm.url.urlInlayHintProvider
import com.intellij.microservices.url.HTTP_SCHEMES
import com.intellij.microservices.url.inlay.UrlPathInlayHintsProviderSemElement
import com.intellij.microservices.url.references.UrlPathContext
import com.intellij.openapi.project.Project
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.uast.uExpression
import com.intellij.psi.registerUastSemProvider
import com.intellij.semantic.SemContributor
import com.intellij.semantic.SemRegistrar

internal class HelidonInlaySemContributor : SemContributor() {
  protected override fun isAvailable(project: Project): Boolean {
    return HelidonCommonUtils.hasHelidonLibrary(project)
  }

  override fun registerSemProviders(registrar: SemRegistrar, project: Project) {
    val pattern = StandardPatterns.or(
      httpRulesMethods(uExpression()),
      anyOfMethod(uExpression()),
      serviceMethodCallPattern(uExpression())
    )
    registrar.registerUastSemProvider(
      UrlPathInlayHintsProviderSemElement.INLAY_HINT_SEM_KEY,
      pattern,
      urlInlayHintProvider(uastUrlPathReferenceInjectorForScheme(HTTP_SCHEMES).withDefaultRootContextProviderFactory {
        val contextElement = it.sourcePsi ?: return@withDefaultRootContextProviderFactory UrlPathContext.supportingSchemes(HTTP_SCHEMES)
        HelidonUrlPathSpecification.getUrlPathContext(contextElement)
      })
    )
  }
}