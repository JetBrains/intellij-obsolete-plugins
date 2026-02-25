/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.pluginSupport.assetPipeline

import com.intellij.lang.LanguageUtil
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.html.HtmlScriptLanguageInjector
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag

class AssetScriptInjector : MultiHostInjector {

  override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
    context as GspGrailsTag
    if (context.namespacePrefix != "asset") return
    val languageToInject = HtmlScriptLanguageInjector.getScriptLanguageToInject(context) ?: return
    if (!LanguageUtil.isInjectableLanguage(languageToInject)) return
    var started = false
    for (child in context.children) {
      if (child is GspOuterHtmlElement) {
        if (!started) {
          registrar.startInjecting(languageToInject)
          started = true
        }
        registrar.addPlace(
            null, null, child, TextRange.create(0, child.getTextLength())
        )
      }
    }
    if (started) registrar.doneInjecting()
  }

  override fun elementsToInjectIn(): List<Class<GspGrailsTag>> = listOf(GspGrailsTag::class.java)
}