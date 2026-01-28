// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.microprofile

import com.intellij.helidon.config.HelidonMPConfigCompletionAutoPopupTestCase

class HelidonMPPropertiesCompletionAutoPopupTest : HelidonMPConfigCompletionAutoPopupTestCase() {

  fun testParametrizedKeyAutoPopupBeforeParameter() {
    configureApplicationProperties("security.secre<caret>s.test.name")
    tester.runWithAutoPopupEnabled { type("t") }
    assertNotNull(myFixture.lookup)
  }

  fun testParametrizedKeyAutoPopupAfterParameter() {

    configureApplicationProperties("security.secrets.test.n<caret>me")
    tester.runWithAutoPopupEnabled { type("a") }
    assertNotNull(myFixture.lookup)
  }

  fun testParametrizedKeyAutoPopupSkipInParameter() {
    configureApplicationProperties("security.secrets.t<caret>.name")
    tester.runWithAutoPopupEnabled { type("e") }
    assertNull(myFixture.lookup)
    tester.runWithAutoPopupEnabled { type("st") }
    assertNull(myFixture.lookup)
  }
}