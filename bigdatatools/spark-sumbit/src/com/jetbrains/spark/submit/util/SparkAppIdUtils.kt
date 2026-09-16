package com.jetbrains.spark.submit.util

object SparkAppIdUtils {
  private val regex = Regex("application_[\\d_]+|app-[\\d_]+(-[\\d_]+)?|(local-[\\d_]+(-[\\d_]+)?)")

  fun lineHasAppId(line: String) = line.contains("application_") || line.contains("app-") || line.contains("local-")

  fun findByRegex(line: String) = regex.find(line)
}