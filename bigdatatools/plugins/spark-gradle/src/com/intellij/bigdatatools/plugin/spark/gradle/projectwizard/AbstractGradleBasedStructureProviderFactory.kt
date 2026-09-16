package com.intellij.bigdatatools.plugin.spark.gradle.projectwizard

import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProviderFactory
import com.jetbrains.bigdatatools.wizard.SparkLanguageType
import icons.GradleIcons
import javax.swing.Icon

abstract class AbstractGradleBasedStructureProviderFactory : BuildSystemBasedStructureProviderFactory {
  override val supportedLanguageType: List<SparkLanguageType> = listOf(SparkLanguageType.JAVA)
  override val presentableName: String
    get() = "Gradle"
  override val icon: Icon
    get() = GradleIcons.Gradle
}
