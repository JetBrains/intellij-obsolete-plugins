package com.jetbrains.spark.submit.run

import com.intellij.execution.configurations.ConfigurationType

abstract class SparkSubmitConfigurationType(val isPySpark: Boolean = false) : ConfigurationType {
  override fun getHelpTopic() = "big.data.tools.spark.submit"
  companion object {
    const val SPARK_ID = "SparkSubmitConfigurationType"
    const val PYSPARK_ID = "PySparkSubmitConfigurationType"
    val ids = listOf(SPARK_ID, PYSPARK_ID)
  }
}