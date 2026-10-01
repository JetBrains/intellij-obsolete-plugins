package com.intellij.dbt.codeInsight

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class DbtReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(Jinja2FunctionCall::class.java).with(DbtModelRefReferenceProvider.isReferenceFunction),
      DbtModelRefReferenceProvider()
    )
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(
      Jinja2VariableReferenceImpl::class.java), DbtModelReferencesProvider())
  }
}