// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.refactoring.rename.PsiElementRenameHandler

class HelidonYamlRenamingTest : HelidonHighlightingTestCase() {
  fun testKeyRenamingVetoed() {
    @Suppress("SpellCheckingInspection")
    assertRenamingVetoed("""
      my:
        in<caret>teger: 42
    """.trimIndent(), false)
  }

  fun testUnresolvedKeyRenamingVetoed() {
    assertRenamingVetoed("""
      so<caret>me:
        INVALID: 42
    """.trimIndent(), true)
  }

  fun testKeyViaPropertyPlaceholderRenamingVetoed() {
    assertRenamingVetoed("""
      my:
        integer: ${"$"}{my.<caret>integer}
    """.trimIndent(), false)
  }

  fun testSystemPropertyPlaceholderRenamingVetoed() {
    assertRenamingVetoed("""
      my:
        integer: ${"$"}{user.<caret>home}
    """, false)
  }

  private fun assertRenamingVetoed(applicationYml: String,
                                   usePlainElementFind: Boolean) {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, applicationYml)
    val element = if (usePlainElementFind) {
      myFixture.file.findElementAt(myFixture.caretOffset)
    }
    else {
      myFixture.elementAtCaret
    }
    assertTrue(PsiElementRenameHandler.isVetoed(element))
  }
}