package com.jetbrains.bigdatatools.wizard

import com.intellij.openapi.extensions.ExtensionPointName

interface SparkProjectLanguageHandler : ItemWithIcon {
  val language: SparkLanguageType

  fun createWizardPanel(): LanguageWizardPanel? = null

  fun createBuildSystemFactories(): List<BuildSystemBasedStructureProviderFactory> =
    BuildSystemBasedStructureProviderFactory.getAll().filter { language in it.supportedLanguageType }


  companion object {
    const val LABEL = "Spark"

    private const val ID = "com.intellij.bigdatatools.bdtFrameworkTemplateFactory"
    private val EP_NAME = ExtensionPointName<SparkProjectLanguageHandler>(ID)

    fun getAll(): List<SparkProjectLanguageHandler> = EP_NAME.extensionList
  }
}