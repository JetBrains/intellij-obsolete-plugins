package com.jetbrains.bigdatatools.wizard

import com.intellij.openapi.extensions.ExtensionPointName
import java.io.File

interface BuildSystemBasedStructureProviderFactory : ItemWithIcon {
  val supportedLanguageType: List<SparkLanguageType>

  fun createProvider(projectDir: File, errorHandler: (Exception) -> Unit): BuildSystemBasedStructureProvider

  companion object {
    private val EP_NAME = ExtensionPointName<BuildSystemBasedStructureProviderFactory>(
      "com.intellij.bigdatatools.buildSystemBasedStructureProviderFactory"
    )

    fun getAll(): List<BuildSystemBasedStructureProviderFactory> = EP_NAME.extensionList
  }
}