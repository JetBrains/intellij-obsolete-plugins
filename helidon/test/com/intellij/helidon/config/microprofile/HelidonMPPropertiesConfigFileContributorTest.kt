// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.microprofile

import com.intellij.helidon.HelidonMPHighlightingTestCase
import com.intellij.helidon.config.isHelidonConfigFile

class HelidonMPPropertiesConfigFileContributorTest : HelidonMPHighlightingTestCase() {

  fun testApplicationProperties() {
    val psiFile = myFixture.addFileToProject("microprofile-config.properties", "")
    assertTrue(isHelidonConfigFile(psiFile))
  }
}