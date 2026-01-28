// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon

import com.intellij.helidon.newproject.HelidonModuleBuilder
import com.intellij.ide.starters.local.StarterModuleBuilder.Companion.setupTestModule
import com.intellij.ide.starters.shared.*
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class HelidonModuleBuilderTest : LightJavaCodeInsightFixtureTestCase4(LightJavaCodeInsightFixtureTestCase.JAVA_11) {
  @Parameter(0)
  lateinit var generateLanguage: String

  @Parameter(1)
  lateinit var generateBuildSystem: String

  companion object {
    @Parameters(name = "{index}: {0} {1}")
    @JvmStatic
    fun generateOptions(): Collection<Array<*>> {
      val data = mutableListOf<Array<*>>()
      data.add(arrayOf("java", "maven"))
      data.add(arrayOf("java", "gradle"))
      data.add(arrayOf("kotlin", "maven"))
      data.add(arrayOf("kotlin", "gradle"))
      return data
    }

    val LANGUAGES: Map<String, StarterLanguage> = mapOf(
      "java" to JAVA_STARTER_LANGUAGE,
      "kotlin" to KOTLIN_STARTER_LANGUAGE,
    )

    val BUILD_SYSTEMS: Map<String, StarterProjectType> = mapOf(
      "maven" to MAVEN_PROJECT,
      "gradle" to GRADLE_PROJECT
    )
  }

  @Test
  fun generateProject() {
    HelidonModuleBuilder().setupTestModule(fixture.module) {
      isCreatingNewProject = true
      language = LANGUAGES[generateLanguage]!!
      projectType = BUILD_SYSTEMS[generateBuildSystem]!!
    }

    expectFile("src/main/resources/META-INF/beans.xml", BEANS_XML)
    expectFile("src/main/resources/META-INF/microprofile-config.properties", MICROPROFILE_CONFIG)
  }

  private fun expectFile(path: String, content: String) {
    fixture.configureFromTempProjectFile(path)
    fixture.checkResult(content)
  }

  private val BEANS_XML: String = """
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd"
           bean-discovery-mode="annotated">
    
    </beans>
  """.trimIndent()

  private val MICROPROFILE_CONFIG: String = """
    server.port=8080
  """.trimIndent()
}