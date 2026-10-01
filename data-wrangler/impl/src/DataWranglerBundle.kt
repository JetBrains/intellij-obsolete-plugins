package com.intellij.dataWrangler.impl

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_FQN = "messages.DataWranglerBundleImpl"

internal object DataWranglerBundle {
  private val BUNDLE = DynamicBundle(DataWranglerBundle::class.java, BUNDLE_FQN)

  @JvmStatic
  fun message(
    key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
    vararg params: Any,
  ): @Nls String {
    return BUNDLE.getMessage(key, *params)
  }
}