// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.helidon.config.HELIDON_APPLICATION_YAML
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.yaml.ConfigYamlAccessor
import org.jetbrains.yaml.psi.YAMLFile

class HelidonYamlAccessorTest : HelidonHighlightingTestCase() {
  // Tests only accessor + helidon binder functionality.
  fun testFindExistingKeyOnElementRelaxed() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      server:
        features:
          print<caret>Details: true
    """.trimIndent())
    val yamlDocument = (myFixture.file as YAMLFile).documents[0]
    val accessor = ConfigYamlAccessor(yamlDocument, HelidonMetaConfigKeyManager.getInstance())
    val yamlKeyValue = accessor.findExistingKey("server.features.print-details")
    assertNotNull(yamlKeyValue)
    assertEquals("printDetails", yamlKeyValue!!.keyText)
  }

  fun testFindExistingParametrizedKey() {
    myFixture.configureByText(HELIDON_APPLICATION_YAML, """
      security:
        secrets:
          dev:
            na<caret>me : "dev"
    """.trimIndent())
    val yamlDocument = (myFixture.file as YAMLFile).documents[0]
    val accessor = ConfigYamlAccessor(yamlDocument, HelidonMetaConfigKeyManager.getInstance())
    val yamlKeyValue = accessor.findExistingKey("security.secrets.*.name")
    assertNotNull(yamlKeyValue)
    assertEquals("name", yamlKeyValue!!.keyText)
  }
}