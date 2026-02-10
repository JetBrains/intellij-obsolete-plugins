// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers

import com.intellij.helidon.constants.HelidonConstants
import com.intellij.helidon.constants.HelidonConstants.HTTP_REQUEST_PATH
import com.intellij.helidon.utils.HelidonCommonUtils
import com.intellij.microservices.jvm.url.uastUrlPathReferenceInjectorForScheme
import com.intellij.microservices.url.FrameworkUrlPathSpecification
import com.intellij.microservices.url.HTTP_SCHEMES
import com.intellij.microservices.url.UrlConversionConstants
import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.references.UrlPathContext
import com.intellij.microservices.url.references.UrlPksParser
import com.intellij.microservices.url.references.extractPathVariable
import com.intellij.microservices.url.PlaceholderSplitEscaper
import com.intellij.microservices.jvm.pathvars.PathVariableReferenceProvider
import com.intellij.microservices.jvm.url.UastUrlPathReferenceProvider
import com.intellij.patterns.PsiJavaPatterns.psiMethod
import com.intellij.patterns.PsiMethodPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.StandardPatterns.or
import com.intellij.patterns.uast.UExpressionPattern
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.psi.CommonClassNames.JAVA_LANG_ITERABLE
import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import org.jetbrains.uast.UExpression

private val handlerMethods: List<String> = listOf("get", "post", "put", "delete", "options", "head", "trace", "any")

internal val httpMethodsPattern: PsiMethodPattern = psiMethod()
  .withName(StandardPatterns.string().oneOf(handlerMethods)).withParameters(
    JAVA_LANG_STRING, HelidonConstants.HANDLER + "...")

internal val anyOfMethodPattern: PsiMethodPattern = psiMethod()
  .withName("anyOf")
  .withParameters(JAVA_LANG_ITERABLE, JAVA_LANG_STRING, HelidonConstants.HANDLER + "...")
internal val registerMethodPattern: PsiMethodPattern = psiMethod()
  .withName("register")
  .withParameters(JAVA_LANG_STRING, HelidonConstants.SERVICE + "...")

internal fun httpRulesMethods(elementPattern: UExpressionPattern<UExpression, *>): UExpressionPattern<*, *> =
  elementPattern.callParameter(0, callExpression().withResolvedMethod(httpMethodsPattern, false))

internal fun anyOfMethod(elementPattern: UExpressionPattern<UExpression, *>): UExpressionPattern<*, *> =
  elementPattern.callParameter(1, callExpression().withResolvedMethod(anyOfMethodPattern, false))

internal fun serviceMethodCallPattern(elementPattern: UExpressionPattern<UExpression, *>): UExpressionPattern<*, *> =
  elementPattern.callParameter(0, callExpression().withResolvedMethod(registerMethodPattern, false))

internal class HelidonReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    val httpRulesMethods = httpRulesMethods(injectionHostUExpression())
    val anyOfMethod = anyOfMethod(injectionHostUExpression())
    val serviceMethodCallPattern = serviceMethodCallPattern(injectionHostUExpression())

    registrar.registerUastReferenceProvider(
      or(httpRulesMethods, serviceMethodCallPattern, anyOfMethod),
      UastUrlPathReferenceProvider { uExpression, psiElement ->
        val injector = uastUrlPathReferenceInjectorForScheme(HTTP_SCHEMES)
          .withDefaultRootContextProviderFactory { HelidonUrlPathSpecification.getUrlPathContext(psiElement) }
        injector.buildReferences(uExpression).forPsiElement(psiElement)
      })

    // path variables
    registrar.registerReferenceProviderByUsage(or(httpRulesMethods, serviceMethodCallPattern, anyOfMethod),
                                               PathVariableReferenceProvider.TO_PATH_VARIABLE)

    val httpRequestPathParam = injectionHostUExpression().callParameter(0,
                                                                        callExpression().withResolvedMethod(
                                                                          psiMethod().withName("param").definedInClass(HTTP_REQUEST_PATH),
                                                                          false))
    registrar.registerUastReferenceProvider(httpRequestPathParam, HelidonHttpRequestPathParamReferenceProvider.INSTANCE)
  }
}

internal object HelidonUrlPathSpecification : FrameworkUrlPathSpecification() {
  override fun getUrlPathContext(declaration: PsiElement): UrlPathContext {
    val parentUrlPaths = HelidonCommonUtils.getParentUrlPaths(declaration)
    val singleContext = UrlPathContext.supportingSchemes(HTTP_SCHEMES)

    return if (parentUrlPaths.size == 0) singleContext
    else singleContext.subContexts(parentUrlPaths.map { path -> HelidonUrlPathSpecification.parsePath(path) })
  }

  override val parser: UrlPksParser = UrlPksParser().apply {
    splitEscaper = { input, pattern -> PlaceholderSplitEscaper.create("{", "}", input, pattern) }
    customPathSegmentExtractor = { segmentStr ->
      extractPathVariable(segmentStr, UrlConversionConstants.SPRING_LIKE_PATH_VARIABLE_BRACES)
      ?: UrlPath.PathSegment.Exact(segmentStr)
    }
    parseQueryParameters = false
  }
}