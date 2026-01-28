// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML

class HelidonYamlCompletionTest : HelidonHighlightingTestCase() {

  fun testNoCompletionForNestedKeysInWrongPosition() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        sockets:
          <caret>
    """.trimIndent())
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertEmpty(lookupElementStrings!!)
  }

  fun testNestedKeyCompletionBeforeAnotherKey() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        <caret>
        backlog: 0
    """.trimIndent())
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertContainsElements(lookupElementStrings!!, listOf(
      "server.host",
      "server.name"
    ))
    assertDoesntContain(lookupElementStrings, listOf("server.backlog"))
    myFixture.type("ho")
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    myFixture.checkResult("""
      server:
        backlog: 0
        host: <caret>
    """.trimIndent())
  }

  fun testNestedKeyCompletionBetweenAnotherKeys() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        host: "localhost"
        <caret>
        port: 8080
    """.trimIndent())
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertContainsElements(lookupElementStrings!!, listOf(
      "server.name"
    ))
    assertDoesntContain(lookupElementStrings, listOf(
      "port",
      "host"
    ))
    myFixture.type("na")
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    myFixture.checkResult("""
      server:
        host: "localhost"
        port: 8080
        name: <caret>
    """.trimIndent())
  }
}