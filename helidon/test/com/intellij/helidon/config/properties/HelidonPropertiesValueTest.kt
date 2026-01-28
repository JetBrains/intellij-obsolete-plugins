// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.helidon.HelidonHighlightingTestCase

class HelidonPropertiesValueTest : HelidonHighlightingTestCase() {

  fun testNoOtherReferencesInValueCompletionForString() {
    configureApplicationProperties("server.host=<caret>")
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertEmpty(lookupElementStrings!!)
  }

  fun testValueCompletion() {
    configureApplicationProperties("security.enabled=<caret>")
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertContainsElements(lookupElementStrings!!, "true", "false")
  }

  fun testValueCompletionEnumClass() {
    configureApplicationProperties("security.provider-policy.type=<caret>")
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertSameElements(lookupElementStrings!!, "class", "composite", "first")
    val elements = myFixture.lookupElements ?: emptyArray()
    assertNotNull(elements)
    val presentation = LookupElementPresentation.renderElement(elements[2])
    assertEquals("first", presentation.itemText)
  }
}