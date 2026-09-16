package com.intellij.bigdatatools.plugin.spark.python.submit

import com.intellij.bigdatatools.sparkSubmit.icons.BigdatatoolsSparkSubmitIcons
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.run.cluster.ClusterSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

class PySparkSubmitConfigurationType : SparkSubmitConfigurationType(isPySpark = true) {
  override fun getIcon(): Icon = BigdatatoolsSparkSubmitIcons.PySpark

  override fun getConfigurationTypeDescription(): String = SparkMessagesBundle.message("configuration.description")

  override fun getId(): String = PYSPARK_ID

  override fun getDisplayName(): String = SparkMessagesBundle.message("configuration.name.python")

  val clusterFactory = ClusterSparkSubmitConfigurationFactory(this)

  override fun getConfigurationFactories() =
    arrayOf(clusterFactory)
}