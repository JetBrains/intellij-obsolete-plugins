package com.jetbrains.bigdatatools.dataproc.util

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.DataprocBundle"

object DataprocMessagesBundle {
  private val instance = DynamicBundle(DataprocMessagesBundle::class.java, BUNDLE)
  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = instance.getMessage(key, *params)
}