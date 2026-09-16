package com.intellij.bigdatatools.plugin.spark.scala.projectwizard

import com.intellij.bigdatatools.plugin.spark.scala.BigdatatoolsPluginSparkScalaIcons
import com.jetbrains.bigdatatools.wizard.SparkLanguageType
import com.jetbrains.bigdatatools.wizard.SparkProjectLanguageHandler
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import scala.collection.immutable.Seq
import org.jetbrains.plugins.scala.project.Versions
import javax.swing.Icon

internal class ScalaSparkProjectLanguageHandler : SparkProjectLanguageHandler {
  override val language: SparkLanguageType
    get() = SparkLanguageType.SCALA

  override val presentableName: String
    get() = WizardMessageBundle.message("bdt.wizard.lang.scala")

  override val icon: Icon
    get() = BigdatatoolsPluginSparkScalaIcons.ScalaSmallLogo

  override fun createWizardPanel() = ScalaWizardPanel()

  object Utils {
    val scalaVersions: Seq<String> by lazy {
      Versions.scala2HardcodedVersions()
    }

    val sbtVersions: Seq<String> by lazy {
      Versions.sbtHardcodedVersions()
    }
  }
}