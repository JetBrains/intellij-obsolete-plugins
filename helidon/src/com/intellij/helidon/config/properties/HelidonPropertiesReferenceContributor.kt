// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

internal class HelidonPropertiesReferenceContributor : PsiReferenceContributor() {

  private val APPLICATION_PROPERTIES: PatternCondition<PsiElement> =
    object : PatternCondition<PsiElement>("isApplicationPropertiesAndHelidon") {
      override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
        val containingFile = element.containingFile.originalFile
        return containingFile is PropertiesFile && isHelidonConfigFile(containingFile)
      }
    }

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registerKeyProviders(registrar)

    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(PropertyValueImpl::class.java).with(APPLICATION_PROPERTIES),
      HelidonPropertiesValueReferenceProvider(),
      PsiReferenceRegistrar.HIGHER_PRIORITY)
  }

  private fun registerKeyProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(PropertyKeyImpl::class.java).with(APPLICATION_PROPERTIES),
      HelidonPropertiesKeyReferenceProvider())

    // Map<Enum/Anything, ..> w/o key hints
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(PropertyKeyImpl::class.java).with(APPLICATION_PROPERTIES)
        .with(object : PatternCondition<PropertyKeyImpl?>("isMapOrIndexedKey") {
          override fun accepts(key: PropertyKeyImpl, context: ProcessingContext): Boolean {
            return MetaConfigKeyReference.findAndStoreMetaConfigKeyIfMatches(key, context,
                                                                             MetaConfigKey.MAP_OR_INDEXED_WITHOUT_KEY_HINTS_CONDITION)
          }
        }),
      HelidonConfigKeyPathReferenceProvider())
  }
}