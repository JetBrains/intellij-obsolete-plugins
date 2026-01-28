// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HelidonConfigValueSearcher

class HelidonPropertiesConfigValueSearcherTest : HelidonHighlightingTestCase() {

  fun testDuplicateEntry() {
    configureApplicationProperties("""
      server.host=host1
      server.host=host2
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.host", true)
    val value = searcher.findValueText()
    assertEquals("host2", value)
  }

  fun testCanonicalEntry() {
    configureApplicationProperties("""
      server.connection-config.keepAlive=false
      server.connection-config.keep-alive=true
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.connection-config.keep-alive", true)
    val value = searcher.findValueText()
    assertEquals("true", value)
  }

  fun testRelaxedEntry() {
    configureApplicationProperties("""
      server.connection-config.keep-alive=true
      server.connection-config.keepAlive=false
    """.trimIndent())
    val searcher = HelidonConfigValueSearcher(module, false, "server.connection-config.keep-alive", true)
    val value = searcher.findValueText()
    assertEquals("false", value)
  }
}