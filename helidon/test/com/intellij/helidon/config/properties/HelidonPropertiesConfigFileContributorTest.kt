// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.isHelidonConfigFile

class HelidonPropertiesConfigFileContributorTest : HelidonHighlightingTestCase() {

  fun testApplicationProperties() {
    val psiFile = myFixture.addFileToProject("application.properties", "")
    assertTrue(isHelidonConfigFile(psiFile))
  }
}