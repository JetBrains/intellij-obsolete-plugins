// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.newproject

import com.intellij.helidon.HelidonIcons
import com.intellij.helidon.utils.HelidonBundle
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

internal class HelidonFileTemplateGroup : FileTemplateGroupDescriptorFactory {
  override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
    val root = FileTemplateGroupDescriptor(HelidonBundle.HELIDON_LIBRARY, HelidonIcons.Helidon)

    root.addTemplate(HELIDON_POM_XML)
    root.addTemplate(HELIDON_MVNW_PROPERTIES)
    root.addTemplate(HELIDON_BUILD_GRADLE)
    root.addTemplate(HELIDON_SETTINGS_GRADLE)
    root.addTemplate(HELIDON_GRADLEW_PROPERTIES)
    root.addTemplate(HELIDON_BEANS_XML)
    root.addTemplate(HELIDON_MICROPROFILE_CONFIG)
    root.addTemplate(HELIDON_APPLICATION_YAML)
    root.addTemplate(HELIDON_LOGGING_PROPERTIES)

    root.addTemplate("helidon-HelloResource-java.java")
    root.addTemplate("helidon-HelloResource-kotlin.kt")

    return root
  }

  companion object {
    const val HELIDON_POM_XML = "helidon-pom.xml"
    const val HELIDON_MVNW_PROPERTIES = "helidon-maven-wrapper.properties"
    const val HELIDON_BUILD_GRADLE = "helidon-build.gradle"
    const val HELIDON_SETTINGS_GRADLE = "helidon-settings.gradle"
    const val HELIDON_GRADLEW_PROPERTIES = "helidon-gradle-wrapper.properties"
    const val HELIDON_BEANS_XML = "helidon-beans.xml"
    const val HELIDON_MICROPROFILE_CONFIG = "helidon-microprofile-config.properties"
    const val HELIDON_LOGGING_PROPERTIES = "helidon-logging.properties"
    const val HELIDON_APPLICATION_YAML = "helidon-application.yaml"
  }
}