package com.jetbrains.bigdatatools.view.messages

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.BinaryFilesSupportBundle"

internal object BinaryFilesSupportBundle {
  private val instance = DynamicBundle(BinaryFilesSupportBundle::class.java, BUNDLE)
  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = instance.getMessage(key, *params)
}