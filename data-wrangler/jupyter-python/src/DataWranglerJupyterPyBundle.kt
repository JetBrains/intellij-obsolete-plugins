package com.intellij.dataWrangler.jupyterPython

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE_FQN = "messages.JupyterPyDataWranglerBundle"

object DataWranglerJupyterPyBundle {

  private val BUNDLE = DynamicBundle(DataWranglerJupyterPyBundle::class.java, BUNDLE_FQN)

  @JvmStatic
  fun message(
    key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
    vararg params: Any,
  ): @Nls String {
    return BUNDLE.getMessage(key, *params)
  }

  @JvmStatic
  fun messagePointer(
    key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
    vararg params: Any,
  ): Supplier<String> {
    return BUNDLE.getLazyMessage(key, *params)
  }
}