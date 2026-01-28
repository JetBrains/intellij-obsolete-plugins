// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.psi.PsiElement

class HelidonPropertiesDocumentationProviderTest : HelidonHighlightingTestCase() {

  fun testQuickNavigateInfo() {
    configureApplicationProperties("server.host<caret>=localhost")
    val docElement: PsiElement = getDocElement()
    val documentationProvider = DocumentationManager.getProviderFromElement(docElement)
    assertEquals(
      """
        <b>server.host</b> [io.helidon.webserver]
        <a href="psi_element://java.lang.String"><code><span style="color:#0000ff;">String</span></code></a>
        """.trimIndent(),
      documentationProvider.getQuickNavigateInfo(docElement, null))
  }

  private fun getDocElement(): PsiElement {
    val docElement = DocumentationManager.getInstance(project).findTargetElement(myFixture.editor, myFixture.file)
    assertNotNull(docElement)
    return docElement
  }
}