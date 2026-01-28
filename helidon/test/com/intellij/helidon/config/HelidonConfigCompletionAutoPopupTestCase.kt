// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.testFramework.fixtures.CompletionAutoPopupTester

abstract class HelidonConfigCompletionAutoPopupTestCase : HelidonHighlightingTestCase() {

  protected lateinit var tester: CompletionAutoPopupTester

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    tester = CompletionAutoPopupTester(myFixture)
  }

  protected fun type(s: String?) {
    tester.typeWithPauses(s)
  }

  override fun runInDispatchThread(): Boolean = false
}