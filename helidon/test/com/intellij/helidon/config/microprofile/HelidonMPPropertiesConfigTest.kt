// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.microprofile

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.grazie.spellcheck.GrazieSpellCheckingInspection
import com.intellij.helidon.HelidonMPHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_PROPERTIES
import com.intellij.helidon.config.HelidonConfigPlaceholderReference
import com.intellij.helidon.config.properties.HelidonPropertiesConfigInspection
import com.intellij.lang.properties.codeInspection.unused.UnusedPropertyInspection
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.util.Consumer
import com.intellij.util.containers.ContainerUtil
import java.io.IOException

@TestDataPath("\$CONTENT_ROOT/testData/config/properties/")
class HelidonMPPropertiesConfigTest : HelidonMPHighlightingTestCase() {

  override fun getTestDirectory(): String = "/config/properties/"

  fun testImplicitUsage() {
    myFixture.enableInspections(UnusedPropertyInspection())
    configureApplicationProperties("server.host=localhost")
    myFixture.testHighlighting()
  }

  fun testNoSpellcheckingForKeys() {
    myFixture.enableInspections(GrazieSpellCheckingInspection())
    configureApplicationProperties("my.map.whatever.8762346shgst623.aukzt28=but <TYPO descr=\"Typo: In word 'thiz'\">thiz</TYPO>")
    myFixture.testHighlighting()
  }

  fun testInspectionAndAnnotatorHighlighting() {
    myFixture.enableInspections(HelidonPropertiesConfigInspection())
    val applicationProperties = myFixture.copyFileToProject("inspectionAndAnnotatorHighlighting.properties",
                                                            HELIDON_APPLICATION_PROPERTIES)
    myFixture.configureFromExistingVirtualFile(applicationProperties)
    myFixture.testHighlighting(true, true, true)
  }

  fun testPlaceholderReferenceCompletion() {
    configureApplicationProperties("""
      my.key=value
      my.completion.key=${"$"}{<caret>}
    """.trimIndent())
    myFixture.completeBasic()
    val variants = myFixture.lookupElementStrings
    assertNotNull(variants)
    assertContainsElements(variants!!, "my.key")
  }

  fun testPlaceholderReferenceResolveToSystemProperty() {
    configureApplicationProperties("my.key=\${os.<caret>name}\n")
    val resolve = resolvePlaceholderReference()
    val result = assertOneElement(resolve)
    val property = assertInstanceOf(result!!.element, PropertyImpl::class.java)
    assertEquals("os.name", property.key)
  }

  fun testPlaceholderReferenceResolveToOtherKey() {
    configureApplicationProperties("""
      my.key=value
      my.ref.key=${"$"}{my.<caret>key}
    """.trimIndent())
    val resolve = resolvePlaceholderReference()
    val result = assertOneElement(resolve)
    val property = assertInstanceOf(result!!.element, PropertyImpl::class.java)
    assertEquals("my.key", property.key)
  }

  @Throws(IOException::class)
  fun testPlaceholderReferenceResolveToKeyInAnyPropertiesFile() {
    val filters = myFixture.tempDirFixture.findOrCreateDir("filters")
    PsiTestUtil.addContentRoot(module, filters)
    Disposer.register(myFixture.testRootDisposable,
                      Disposable { PsiTestUtil.removeContentEntry(module, filters) })
    val myPropertiesFile = myFixture.addFileToProject("filters/myProperties.properties", "someKeyAnywhere=someValue")
    val myPropertiesFile2 = myFixture.addFileToProject("filters/myProperties2.properties", "someKeyAnywhere=someValue2")
    configureApplicationProperties("my.key=\${someKey<caret>Anywhere}")
    val resolve = resolvePlaceholderReference()
    assertSize(2, resolve)
    assertUnorderedCollection(resolve,
                              Consumer {
                                val property = assertInstanceOf(it!!.element, PropertyImpl::class.java)
                                assertEquals("someKeyAnywhere", property.key)
                                assertEquals("someValue", property.value)
                                assertEquals(myPropertiesFile, property.propertiesFile.containingFile)
                              },
                              Consumer {
                                val property = assertInstanceOf(it!!.element, PropertyImpl::class.java)
                                assertEquals("someKeyAnywhere", property.key)
                                assertEquals("someValue2", property.value)
                                assertEquals(myPropertiesFile2, property.propertiesFile.containingFile)
                              })
  }

  fun testKeyRenamingVetoed() {
    assertRenamingVetoed("server.<caret>host=localhost\n", true)
  }

  fun testUserPropertyPlaceholderRenamingPossible() {
    assertRenamingVetoed("""
      my.host.key=localhost
      my.host=${'$'}{my.ho<caret>st.key}
    """.trimIndent(), false)
  }

  private fun assertRenamingVetoed(applicationProperties: String,
                                   prohibited: Boolean) {
    configureApplicationProperties(applicationProperties)
    val element = myFixture.elementAtCaret
    assertEquals(prohibited, PsiElementRenameHandler.isVetoed(element))
  }

  fun testKeyCompletionLookupElements() {
    Registry.get("ide.completion.variant.limit").setValue("1500", testRootDisposable)
    configureApplicationProperties("""
      server.host=localhost
      server.<caret>
    """.trimIndent())
    myFixture.completeBasic()
    val lookupStrings = myFixture.lookupElementStrings
    assertNotNull(lookupStrings)
    assertContainsElements(lookupStrings!!, "server.backlog", "server.name")
    assertDoesntContain(lookupStrings, "server.host")
  }

  fun testKeyCompletionInsertsDelimiterTail() {
    configureApplicationProperties("server.t<caret>")
    myFixture.completeBasic()
    myFixture.type("ls.enabled")
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    myFixture.checkResult("server.tls.enabled=<caret>", true)
  }

  fun testKeyListTypeReferenceRange() {
    configureApplicationProperties("my.<caret>list=one,two,three\n")
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertEquals("my.list", reference.canonicalText)
    assertEquals(TextRange.from(0, 7), reference.rangeInElement)
  }

  fun testParametrizedKeyCompletionWithParameter() {
    configureApplicationProperties("security.secrets.test.n<caret>")
    myFixture.completeBasic()
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    myFixture.checkResult("security.secrets.test.name=<caret>")
  }

  private fun resolvePlaceholderReference(): Array<ResolveResult?> {
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    return getPlaceholderReferenceResolveResults(reference)
  }

  private fun getPlaceholderReferenceResolveResults(reference: PsiReference): Array<ResolveResult?> {
    val multiReference = assertInstanceOf(reference, PsiMultiReference::class.java)
    val placeholderReference = ContainerUtil.findInstance(multiReference.references, HelidonConfigPlaceholderReference::class.java)
    assertNotNull(placeholderReference)
    return placeholderReference.multiResolve(false)
  }
}