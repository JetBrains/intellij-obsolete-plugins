// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.openapi.util.registry.Registry

class HelidonYamlKeyCompletionTest : HelidonHighlightingTestCase() {

  fun testNoCompletionInComments() {
    doCompletion("# <caret>")
    assertEmpty(myFixture.lookupElementStrings!!.filter { it != "\$schema: " })
    doCompletion("""
      my: 
      # <caret>  key:
    """.trimIndent())
    assertEmpty(myFixture.lookupElementStrings!!)
    doCompletion("""
      my: 
        key: 22 # <caret>
    """.trimIndent())
    assertEmpty(myFixture.lookupElementStrings!!)
  }

  fun testExistedSingleKey() {
    doCompletion("""
      server:
        ho<caret>:
    """.trimIndent())
    myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR)
    myFixture.checkResult("""
      server:
        host: <caret>
    """.trimIndent())
  }

  fun testMultiDocumentEmptyBeforeDelimiter() {
    disableCompletionVariantsLimitForTest()
    doCompletion("""
      <caret>
      ---
    """.trimIndent())
    myFixture.type("name")
    myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR)
    myFixture.checkResult("""
      security:
        provider-policy:
          class-name: <caret>
      ---
    """.trimIndent())
  }

  fun testCompletionOnLineStart() {
    disableCompletionVariantsLimitForTest()
    doCompletion("""
      ---
      server:
      <caret>
    """.trimIndent())
    myFixture.type("host")
    myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR)
    myFixture.checkResult("""
      ---
      server:
        host: <caret>
    """.trimIndent())
  }

  fun testGivenPrefixKeyCompletionVariants() {
    doCompletion("""
      server:
        <caret>
    """.trimIndent())
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertContainsElements(lookupElementStrings!!, "server.host", "server.backlog", "server.port")
  }

  fun testGivenPrefixKeyFilterExistingCompletionVariants() {
    doCompletion("""
      server:
        tls:
          enabled: true
          <caret>
    """.trimIndent())
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertContainsElements(lookupElementStrings!!, "server.tls.client-auth", "server.tls.session-cache-size")
  }

  fun testGivenPrefixKeyWithExistingKeyTextCompletionVariants() {
    doCompletion("""
      server:
        tls:
          ci<caret>
    """.trimIndent())
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertSameElements(lookupElementStrings!!, "server.tls.cipher-suite")
  }

  fun testGivenPrefixKeyMultipleDocumentsCompletionVariants() {
    doCompletion("""
      server:
        tls:
          enabled: true
      ---
      server:
        tls:
          ci<caret>
    """.trimIndent())
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertSameElements(lookupElementStrings!!, "server.tls.cipher-suite")
  }

  private fun disableCompletionVariantsLimitForTest() {
    Registry.get("ide.completion.variant.limit").setValue("1500", testRootDisposable)
  }

  private fun doCompletion(text: String) {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, text)
    myFixture.completeBasic()
  }
}