// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections

import com.intellij.codeInspection.inheritance.ImplicitSubclassProvider
import com.intellij.guice.GuiceBundle
import com.intellij.guice.constants.GuiceAnnotations.TRANSACTIONAL
import com.intellij.guice.utils.GuiceUtils
import com.intellij.java.analysis.JavaAnalysisBundle
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

// see: https://github.com/google/guice/wiki/Transactions
// these methods must be on objects that were created by Guice and they must not be private.
internal class GuiceImplicitSubclassProvider : ImplicitSubclassProvider() {
  override fun getSubclassingInfo(psiClass: PsiClass): SubclassingInfo? {
    val methodsToOverride = HashMap<PsiMethod, OverridingInfo>().apply {
      for (method in psiClass.methods) {
        if (method.hasAnnotation(TRANSACTIONAL)) {
          put(method, OverridingInfo(GuiceBundle.message("ImplicitSubclassInspection.display.forMethod.annotated")))
        }
      }
    }
    if (methodsToOverride.isNotEmpty()) {
      return SubclassingInfo(JavaAnalysisBundle.message("inspection.implicit.subclass.display.forClass", psiClass.name), methodsToOverride)
    }
    return null
  }

  override fun isApplicableTo(psiClass: PsiClass): Boolean = GuiceUtils.isInstantiable(psiClass)
}