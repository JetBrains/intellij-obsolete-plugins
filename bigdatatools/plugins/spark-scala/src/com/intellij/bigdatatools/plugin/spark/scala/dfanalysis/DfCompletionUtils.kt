package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.util.SparkAssistanceRegistry

object DfCompletionUtils {
  fun isEnabled() = SparkAssistanceRegistry.ENABlE_DATAFRAME_ASSISTANCE.asBoolean()
}