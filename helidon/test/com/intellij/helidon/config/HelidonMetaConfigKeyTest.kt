// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.ConfigKeyDeclarationPsiElement
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKey.DescriptionText
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons

class HelidonMetaConfigKeyTest : HelidonMetaConfigKeyTestCase() {
  fun testGetIcon() {
    val plain = createKey("plain", PsiTypes.booleanType(), MetaConfigKey.AccessType.NORMAL)
    assertEquals(IconManager.getInstance().getPlatformIcon(PlatformIcons.Property), plain.presentation.icon)
  }

  fun testDescriptionTextShortText() {
    assertEquals("Text Without Dot", DescriptionText("Text Without Dot").shortText)
    assertEquals("Text With Dot", DescriptionText("Text With Dot.").shortText)
    assertEquals("Text With e.g. and Dot", DescriptionText("Text With e.g. and Dot.").shortText)
    assertEquals("Text With Dot", DescriptionText("Text With Dot. And 2nd sentence.").shortText)
  }

  fun testKeyGotoDeclarationAction() {
    configureApplicationProperties("server.ho<caret>st=localhost")
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val resolve = reference.resolve()
    val configKey = assertInstanceOf(resolve, ConfigKeyDeclarationPsiElement::class.java)
    val setterMethod = assertInstanceOf(configKey.navigationElement, PsiMethod::class.java)
    assertEquals("host", setterMethod.name)
  }

  fun testMetaConfigKeyDeclarationResolveToPropertySetter() {
    val element = assertConfigKeyDeclarationPsiElement(
      getConfigKeyDeclarationPsiElement("server.host"),
      "server.host",
      "io.helidon.webserver.ListenerConfig.Builder",
      "String")
    val setterTarget = assertInstanceOf(element.navigationElement, PsiMethod::class.java)
    assertEquals("host", setterTarget.name)
  }

  fun testMetaConfigKeyDeclarationResolveToClass() {
    val element = assertConfigKeyDeclarationPsiElement(
      getConfigKeyDeclarationPsiElement("server.host"),
      "server.host",
      "io.helidon.webserver.ListenerConfig.Builder",  // no sourceType
      "String")
    val classMethod = assertInstanceOf(element.navigationElement, PsiMethod::class.java)
    assertEquals("io.helidon.webserver.ListenerConfig.BuilderBase", classMethod.containingClass?.qualifiedName)
  }

  private fun getConfigKeyDeclarationPsiElement(configKey: String): ConfigKeyDeclarationPsiElement {
    val key = HelidonMetaConfigKeyManager.getInstance().findCanonicalApplicationMetaConfigKey(module, configKey)
    assertNotNull(configKey, key)
    return assertInstanceOf(key!!.declaration, ConfigKeyDeclarationPsiElement::class.java)
  }

  private fun assertConfigKeyDeclarationPsiElement(psiElement: PsiElement,
                                                   name: String,
                                                   presentableText: String,
                                                   typeText: String): ConfigKeyDeclarationPsiElement {
    val declarationPsiElement = assertInstanceOf(psiElement,
                                                 ConfigKeyDeclarationPsiElement::class.java)
    assertEquals(name, declarationPsiElement.name)
    assertEquals(presentableText, declarationPsiElement.presentableText)
    assertEquals(typeText, declarationPsiElement.type!!.presentableText)
    return declarationPsiElement
  }
}