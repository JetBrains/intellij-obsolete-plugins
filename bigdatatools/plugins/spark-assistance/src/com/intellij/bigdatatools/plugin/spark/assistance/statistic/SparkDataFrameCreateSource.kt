package com.intellij.bigdatatools.plugin.spark.assistance.statistic

enum class SparkDataFrameCreateSource {
  EXPLICIT_SCHEMA,
  UNKNOWN_SCHEMA,
  INLAY_SCHEMA,
  PARSE_FILE_SCHEMA,
  DATAFRAME_TRANSFORM,
  DATAFRAME_TRANSFORM_PARTIAL
}