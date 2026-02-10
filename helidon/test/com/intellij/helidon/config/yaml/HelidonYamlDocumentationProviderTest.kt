// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.psi.PsiElement

class HelidonYamlDocumentationProviderTest : HelidonHighlightingTestCase() {
  fun testQuickNavigateInfo() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        ho<caret>st: "localhost"
    """.trimIndent())
    val docElement = getDocElement()
    val documentationProvider = DocumentationManager.getProviderFromElement(docElement)
    assertEquals(
      """
        <b>server.host</b> [io.helidon.webserver]
        <a href="psi_element://java.lang.String"><code><span style="color:#0000ff;">String</span></code></a>
      """.trimIndent(),
      documentationProvider.getQuickNavigateInfo(docElement, null))
  }

  fun testQuickNavigateInfoPlaceholder() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        ho<caret>st: "localhost"
      my:
        key: ${"$"}{server.ho<caret>st}
    """.trimIndent())
    val docElement = getDocElement()
    val documentationProvider = DocumentationManager.getProviderFromElement(docElement)
    assertEquals("\"localhost\" [application.yaml]", documentationProvider.getQuickNavigateInfo(docElement, null))
  }

  fun testGenerateDoc() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        ho<caret>st: "localhost"
    """.trimIndent())
    val docElement = getDocElement()
    val documentationProvider = DocumentationManager.getProviderFromElement(docElement)
    assertEquals(
      """
       <div class='definition'><pre><b>server.host</b><br><a href="psi_element://java.lang.String"><code><span style="color:#0000ff;">String</span></code></a></pre></div><div class='content'>Host of the default socket. Defaults to all host addresses (`0.0.0.0`).

        @return host address to listen on (for the default socket)<br><br></div><table class='sections'><tr><td valign='top' class='section'><p>Default:</td><td valign='top'><pre>0.0.0.0</pre></td></table>
      """.trimIndent(),
      documentationProvider.generateDoc(docElement, getOriginalElement()))
  }

  private fun getOriginalElement(): PsiElement? {
    return myFixture.file.findElementAt(myFixture.caretOffset)
  }

  private fun getDocElement(): PsiElement {
    val docElement = DocumentationManager.getInstance(project).findTargetElement(myFixture.editor,
                                                                                 myFixture.file)
    assertNotNull(docElement)
    return docElement
  }
}