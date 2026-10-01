package com.intellij.aidebugger.common

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object AiDebuggerBundle {
  private const val BUNDLE = "messages.AiDebuggerCommonBundle"

  private val INSTANCE: DynamicBundle = DynamicBundle(AiDebuggerBundle::class.java, BUNDLE)

  @Nls
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return INSTANCE.getMessage(key, *params)
  }
}