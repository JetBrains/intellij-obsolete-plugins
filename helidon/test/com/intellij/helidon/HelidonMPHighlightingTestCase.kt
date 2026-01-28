// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon

import com.intellij.helidon.config.HELIDON_MP_CONFIG_PROPERTIES
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightProjectDescriptor

abstract class HelidonMPHighlightingTestCase : HelidonHighlightingTestCase() {

  override fun getProjectDescriptor(): LightProjectDescriptor = HELIDON_PROJECT

  override fun configureApplicationProperties(text: String): PsiFile = myFixture.configureByText(HELIDON_MP_CONFIG_PROPERTIES, text)
}