// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.helidon.config.HelidonConfigPlaceholderReference
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiNamedElement
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.jetbrains.jsonSchema.extension.JsonWidgetSuppressor
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.util.*

@Suppress("SSBasedInspection")
@TestDataPath("\$CONTENT_ROOT/testData/config/yaml/")
class HelidonYamlConfigTest : HelidonHighlightingTestCase() {
  override fun getTestDirectory(): String = "/config/yaml/"

  fun testJsonWidgetIsSuppressed() {
    val virtualFile = myFixture.configureByText(HELIDON_APPLICATION_YAML, "anything").virtualFile!!
    val suppressors = JsonWidgetSuppressor.EXTENSION_POINT_NAME.extensions
    assertTrue(Arrays.stream(suppressors).anyMatch { s -> s.suppressSwitcherWidget(virtualFile, project) })
  }

  fun testInspectionAndAnnotatorHighlighting() {
    myFixture.enableInspections(HelidonYamlConfigInspection())
    val applicationYaml = myFixture.copyFileToProject("inspectionAndAnnotatorHighlighting.yml",
                                                      HELIDON_APPLICATION_YAML)
    myFixture.configureFromExistingVirtualFile(applicationYaml)
    myFixture.testHighlighting(true, true, true)
  }

  fun testParametrizedKeyHighlighting() {
    doHighlighting("""
      security:
        secrets:
          <info descr="REGULAR_ITALIC_ATTRIBUTES">dev</info>:
            name: "dev"
    """.trimIndent(), myFixture)
  }

  fun testConfigKeyReferenceResolve() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        shutdown-hook<caret>: false
    """.trimIndent())

    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertEquals("shutdown-hook", reference.canonicalText)

    val configKeyReference = assertInstanceOf(reference, MetaConfigKeyReference::class.java)
    assertEquals("server.shutdown-hook: false", configKeyReference.referenceDisplayText)
    val resolve = configKeyReference.resolve()
    val configKeyDeclarationPsiElement = assertInstanceOf(resolve, PsiNamedElement::class.java)
    assertEquals("server.shutdown-hook", configKeyDeclarationPsiElement.name)
  }

  fun testConfigKeyReferenceRange() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        ho<caret>st: "localhost"
    """.trimIndent())

    val configKeyReference = assertInstanceOf(myFixture.getReferenceAtCaretPositionWithAssertion(), MetaConfigKeyReference::class.java)
    assertEquals(TextRange.create(0, 4), configKeyReference.rangeInElement)
  }

  fun testPlaceholderReferenceCompletion() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      my:
        host: "localhost"
      server:
        host: ${'$'}{<caret>}
    """.trimIndent())
    myFixture.completeBasic()

    assertContainsElements(myFixture.lookupElementStrings!!,
                           "my.host",)
  }

  fun testPlaceholderReferenceResolveToOtherKey() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      my:
        key: value
        ref:
          key: ${"$"}{my.<caret>key}
    """.trimIndent())

    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertInstanceOf(reference, HelidonConfigPlaceholderReference::class.java)

    assertEquals("my.key", reference.canonicalText)
    val yamlKeyValue = assertInstanceOf(reference.resolve(), YAMLKeyValue::class.java)
    assertEquals("key", yamlKeyValue.keyText)
  }

  companion object {
    fun doHighlighting(applicationYamlContent: String, codeInsightTestFixture: CodeInsightTestFixture) {
      codeInsightTestFixture.enableInspections(HelidonYamlConfigInspection())
      codeInsightTestFixture.configureByText(HELIDON_APPLICATION_YAML,
                                             applicationYamlContent.trimIndent())
      codeInsightTestFixture.testHighlighting(true, true, true)
    }
  }
}