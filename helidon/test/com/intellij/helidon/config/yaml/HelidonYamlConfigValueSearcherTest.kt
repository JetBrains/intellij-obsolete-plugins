// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.helidon.config.HelidonConfigValueSearcher

class HelidonYamlConfigValueSearcherTest : HelidonHighlightingTestCase() {
  fun testDuplicateEntry() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        host: host1
        host: host2
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.host", true)
    val value = searcher.findValueText()
    assertEquals("host2", value)
  }

  fun testCanonicalEntry() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        connection-config:
          keep-alive: true
          keepAlive: false
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.connection-config.keep-alive", true)
    val value = searcher.findValueText()
    assertEquals("false", value)
  }

  fun testRelaxedEntry() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        connection-config:
          keep-alive: true
          keepAlive: false
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.connection-config.keep-alive", true)
    val value = searcher.findValueText()
    assertEquals("false", value)
  }

  fun testMultiDocument() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        connection-config:
          keep-alive: true
      ---
      server:
        connection-config:
          keep-alive: false
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.connection-config.keep-alive", true)
    val value = searcher.findValueText()
    assertEquals("false", value)
  }

  fun testFindIntegerConfigurationValue() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        backlog: 100
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.backlog")
    val value = searcher.findValueText()
    assertEquals("100", value)
  }

  fun testFindSplitIntegerConfigurationValue() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        backlog: 10_0
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.backlog")
    val value = searcher.findValueText()
    assertEquals("100", value)
  }
}