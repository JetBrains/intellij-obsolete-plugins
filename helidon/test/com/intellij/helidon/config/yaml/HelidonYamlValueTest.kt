// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML

class HelidonYamlValueTest : HelidonHighlightingTestCase() {
  fun testValueCompletion() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      security:
        provider-policy:
          type: <caret>
    """.trimIndent())
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertSameElements(lookupElementStrings!!, "class", "composite", "first")
  }

  fun testBooleanValueCompletion() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      security:
        enabled: <caret>
    """.trimIndent())
    myFixture.completeBasic()
    val lookupElementStrings = myFixture.lookupElementStrings
    assertNotNull(lookupElementStrings)
    assertSameElements(lookupElementStrings!!, "true", "false")
  }
}