package com.intellij.bigdatatools.plugin.spark.maven.projectwizard

import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProviderFactory
import com.jetbrains.bigdatatools.wizard.SparkLanguageType
import icons.MavenIcons
import javax.swing.Icon

abstract class AbstractMavenBasedStructureProviderFactory : BuildSystemBasedStructureProviderFactory {
  override val supportedLanguageType: List<SparkLanguageType> = listOf(SparkLanguageType.JAVA)

  override val presentableName: String
    get() = "Maven"
  override val icon: Icon
    get() = MavenIcons.MavenProject
}