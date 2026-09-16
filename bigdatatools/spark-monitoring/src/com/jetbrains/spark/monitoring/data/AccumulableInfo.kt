package com.jetbrains.spark.monitoring.data

data class AccumulableInfo(
  val id: Long = 0,
  val name: String = "",
  val update: String = "",
  val value: String = ""
)