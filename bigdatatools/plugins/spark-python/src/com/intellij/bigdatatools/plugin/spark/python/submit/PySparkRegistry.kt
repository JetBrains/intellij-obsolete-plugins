package com.intellij.bigdatatools.plugin.spark.python.submit

import com.intellij.bigdatatools.plugin.spark.assistance.util.SparkAssistanceRegistry
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.registry.Registry

object PySparkRegistry {
  val ENABlE_PYSPARK_COMPLETION: Boolean
    get() = SparkAssistanceRegistry.ENABlE_DATAFRAME_ASSISTANCE.asBoolean()

  val DEBUG_HIGHLIGHT: Boolean
    get() = try {
      Registry.get("spark.python.debug.highlight.schema").asBoolean()
    }
    catch (t: Throwable) {
      thisLogger().warn(t)
      false
    }
}