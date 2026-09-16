package com.intellij.bigdatatools.plugin.spark.scala.projectwizard

import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProvider
import java.io.File

class SparkSbtStructureProviderFactory : AbstractSbtBasedStructureProviderFactory() {
  override fun createProvider(projectDir: File, errorHandler: (Exception) -> Unit): BuildSystemBasedStructureProvider =
    SbtBasedStructureProvider(projectDir, errorHandler)
}