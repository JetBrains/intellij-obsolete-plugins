// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

internal val APPLICATION_YAML_CONDITION: PatternCondition<PsiElement> = object : PatternCondition<PsiElement>("isApplicationPropertiesAndHelidon") {
  override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
    return isInsideApplicationYamlFile(element)
  }
}

internal class HelidonYamlReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      psiElement(YAMLKeyValue::class.java).with(APPLICATION_YAML_CONDITION),
      HelidonYamlKeyReferenceProvider())
    registrar.registerReferenceProvider(
      psiElement(YAMLScalar::class.java).with(APPLICATION_YAML_CONDITION),
      HelidonYamlValueReferenceProvider())
  }
}