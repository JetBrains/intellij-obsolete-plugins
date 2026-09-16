package com.intellij.bigdatatools.plugin.spark.java.wizard

import com.intellij.icons.AllIcons
import com.jetbrains.bigdatatools.wizard.SparkLanguageType
import com.jetbrains.bigdatatools.wizard.SparkProjectLanguageHandler
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import javax.swing.Icon

class JavaSparkProjectLanguageHandler : SparkProjectLanguageHandler {
  override val language: SparkLanguageType = SparkLanguageType.JAVA
  override val presentableName: String = WizardMessageBundle.message("bdt.wizard.lang.java")
  override val icon: Icon = AllIcons.FileTypes.Java


}
