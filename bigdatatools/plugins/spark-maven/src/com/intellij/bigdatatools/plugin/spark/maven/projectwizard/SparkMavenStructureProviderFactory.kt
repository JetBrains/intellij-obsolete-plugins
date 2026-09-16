package com.intellij.bigdatatools.plugin.spark.maven.projectwizard

import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProvider
import java.io.File

class SparkMavenStructureProviderFactory : AbstractMavenBasedStructureProviderFactory() {
  override fun createProvider(projectDir: File, errorHandler: (Exception) -> Unit): BuildSystemBasedStructureProvider =
    MavenBasedStructureProvider(projectDir, errorHandler)
}