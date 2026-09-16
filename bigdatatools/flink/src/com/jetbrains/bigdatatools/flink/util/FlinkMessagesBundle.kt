package com.jetbrains.bigdatatools.flink.util

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.FlinkBundle"

object FlinkMessagesBundle {
  private val instance = DynamicBundle(FlinkMessagesBundle::class.java, BUNDLE)
  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = instance.getMessage(key, *params)
}