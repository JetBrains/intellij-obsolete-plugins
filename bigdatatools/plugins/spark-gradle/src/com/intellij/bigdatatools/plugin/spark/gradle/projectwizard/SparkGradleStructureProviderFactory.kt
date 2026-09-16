package com.intellij.bigdatatools.plugin.spark.gradle.projectwizard

import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProvider
import java.io.File

class SparkGradleStructureProviderFactory : AbstractGradleBasedStructureProviderFactory() {
  override fun createProvider(projectDir: File, errorHandler: (Exception) -> Unit): BuildSystemBasedStructureProvider =
    GradleBasedStructureProvider(projectDir, errorHandler)
}