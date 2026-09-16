package com.intellij.bigdatatools.plugin.spark.python.submit.wizard

import com.intellij.bigdatatools.sparkSubmit.icons.BigdatatoolsSparkSubmitIcons
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.python.newProjectWizard.PyV3ProjectBaseGenerator
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

class PyV3SparkGenerator : PyV3ProjectBaseGenerator<PyV3SparkSettings>(PyV3SparkSettings(), PyV3SparkUI) {
  override fun getName(): String = SparkMessagesBundle.message("pyspark")
  override fun getLogo(): Icon = BigdatatoolsSparkSubmitIcons.PySpark

  override val projectTypeForStatistics: @NlsSafe String = "com.intellij.bigdatatools.plugin.spark.python.submit.wizard.PySparkProjectGenerator"
}