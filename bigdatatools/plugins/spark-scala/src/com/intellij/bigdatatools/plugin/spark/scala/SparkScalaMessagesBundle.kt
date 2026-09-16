package com.intellij.bigdatatools.plugin.spark.scala

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.SparkScalaBundle"

internal object SparkScalaMessagesBundle {
  private val instance = DynamicBundle(SparkScalaMessagesBundle::class.java, BUNDLE)

  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): @Nls String = instance.getMessage(key, *params)
}