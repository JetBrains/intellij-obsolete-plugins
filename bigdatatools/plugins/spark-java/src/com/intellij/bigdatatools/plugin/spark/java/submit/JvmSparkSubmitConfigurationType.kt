package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.bigdatatools.sparkSubmit.icons.BigdatatoolsSparkSubmitIcons
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.run.cluster.ClusterSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.run.local.LocalSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.run.ssh.SshSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

class JvmSparkSubmitConfigurationType : SparkSubmitConfigurationType(isPySpark = false) {
  override fun getIcon(): Icon = BigdatatoolsSparkSubmitIcons.Spark

  override fun getConfigurationTypeDescription(): String = SparkMessagesBundle.message("configuration.description")

  override fun getId(): String = SPARK_ID

  override fun getDisplayName(): String = SparkMessagesBundle.message("configuration.name")

  val clusterFactory = ClusterSparkSubmitConfigurationFactory(this)
  val localFactory = LocalSparkSubmitConfigurationFactory(this)
  val sshFactory = SshSparkSubmitConfigurationFactory(this)

  override fun getConfigurationFactories() =
    arrayOf(clusterFactory, localFactory, sshFactory)
}