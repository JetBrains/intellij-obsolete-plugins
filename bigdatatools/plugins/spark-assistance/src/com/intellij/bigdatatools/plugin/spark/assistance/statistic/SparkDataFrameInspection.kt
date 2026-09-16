package com.intellij.bigdatatools.plugin.spark.assistance.statistic

enum class SparkDataFrameInspection {
  NON_EXISTING_COLUMN,
  CONFLICT,
  COLUMN_EXISTS,
  ALIAS_EXISTS,
  SUSPICIOUS_CAST,
  OTHER
}