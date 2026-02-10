// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.helidon.config.HelidonConfigCompletionAutoPopupTestCase

class HelidonYamlCompletionAutoPopupTest : HelidonConfigCompletionAutoPopupTestCase() {
  fun testParametrizedKeyAutoPopupInParent() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      security:
        secret<caret>s:
          dev:
            name:
    """.trimIndent())
    tester.runWithAutoPopupEnabled { type("s") }
    assertNotNull(myFixture.lookup)
  }

  fun testParametrizedKeyAutoPopupInChild() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      security:
        secrets:
          dev:
            na<caret>
    """.trimIndent())
    tester.runWithAutoPopupEnabled { type("m") }
    assertNotNull(myFixture.lookup)
  }

  fun testParametrizedKeyAutoPopupSkipInParameterWithChild() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      security:
        secrets:
          <caret>:
            name:
    """.trimIndent())
    tester.runWithAutoPopupEnabled { type("d") }
    assertNull(myFixture.lookup)
    tester.runWithAutoPopupEnabled { type("ev") }
    assertNull(myFixture.lookup)
  }
}