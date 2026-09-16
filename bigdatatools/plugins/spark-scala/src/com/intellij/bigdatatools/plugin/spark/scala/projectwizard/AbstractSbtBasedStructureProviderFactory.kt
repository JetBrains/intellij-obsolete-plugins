package com.intellij.bigdatatools.plugin.spark.scala.projectwizard

import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProviderFactory
import com.jetbrains.bigdatatools.wizard.SparkLanguageType
import org.jetbrains.sbt.icons.Icons
import javax.swing.Icon

abstract class AbstractSbtBasedStructureProviderFactory : BuildSystemBasedStructureProviderFactory {
  override val supportedLanguageType: List<SparkLanguageType> = listOf(SparkLanguageType.SCALA)
  override val presentableName: String
    get() = "SBT"
  override val icon: Icon
    get() = Icons.SBT
}