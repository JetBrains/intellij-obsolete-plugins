// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKey.DescriptionText
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons

class HelidonMetaConfigKeyLookupElementBuilderTest : HelidonHighlightingTestCase() {
  fun testPresentation() {
    val dummyClass = myFixture.addClass("public class Dummy{}")
    val key = HelidonMetaConfigKey("name",
                                   dummyClass,
                                   MetaConfigKey.DeclarationResolveResult.PROPERTY,
                                   PsiTypesUtil.getClassType(dummyClass),
                                   DescriptionText("""
                                Description text e.g. documentation, but 
                                line break and last dot will be removed. Only first sentence will be shown.
                              """.trimIndent()),
                                   MetaConfigKey.Deprecation.NOT_DEPRECATED,
                                   "A very long default value which will be cut to a maximum of 60 characters",
                                   MetaConfigKey.AccessType.NORMAL)
    val lookupElement = key.presentation.lookupElement
    val presentation = LookupElementPresentation.renderElement(lookupElement)
    assertEquals(IconManager.getInstance().getPlatformIcon(PlatformIcons.Property), presentation.icon)
    assertEquals("name", presentation.itemText)
    assertFalse(presentation.isItemTextBold)
    assertFalse(presentation.isStrikeout)
    val fragments = presentation.tailFragments
    assertSize(2, fragments)
    assertEquals("=A very long default value which will be cut to a maximum of…", fragments[0]!!.text)
    val descriptionFragment = fragments[1]
    assertEquals(" (Description text e.g. documentation, but line break and last dot will be removed)",
                 descriptionFragment!!.text)
    assertTrue(descriptionFragment.isGrayed)
    assertEquals("Dummy", presentation.typeText)
    assertFalse(presentation.isTypeGrayed)
  }
}