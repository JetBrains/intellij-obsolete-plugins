// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.PsiTypesUtil

abstract class HelidonMetaConfigKeyTestCase : HelidonHighlightingTestCase() {
  protected fun createKey(key: String, psiType: PsiType?, accessType: MetaConfigKey.AccessType): MetaConfigKey {
    val dummyDeclaration: FakePsiElement = object : FakePsiElement() {
      override fun getParent(): PsiElement {
        return getMapType().resolve()!! // dummy declaration
      }
    }
    return HelidonMetaConfigKey(key,
                                dummyDeclaration,
                                MetaConfigKey.DeclarationResolveResult.PROPERTY,
                                psiType,
                                MetaConfigKey.DescriptionText("desc"),
                                MetaConfigKey.Deprecation.NOT_DEPRECATED, null,
                                accessType)
  }

  protected fun getMapType(): PsiClassType = getType(CommonClassNames.JAVA_UTIL_MAP)

  private fun getType(fqn: String): PsiClassType {
    val fqnClass = myFixture.findClass(fqn)
    assertNotNull(fqnClass)
    return PsiTypesUtil.getClassType(fqnClass)
  }
}