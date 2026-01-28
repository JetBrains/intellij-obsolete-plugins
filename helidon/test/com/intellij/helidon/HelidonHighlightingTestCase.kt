// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon

import com.intellij.helidon.config.HELIDON_APPLICATION_PROPERTIES
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

abstract class HelidonHighlightingTestCase : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return HELIDON_PROJECT
    }

    override fun getTestDataPath(): String? = "testData" + getTestDirectory()

    protected open fun getTestDirectory(): String = "Override_getTestDirectory"

    protected open fun configureApplicationProperties(text: String): PsiFile {
        return myFixture.configureByText(HELIDON_APPLICATION_PROPERTIES, text)
    }
}