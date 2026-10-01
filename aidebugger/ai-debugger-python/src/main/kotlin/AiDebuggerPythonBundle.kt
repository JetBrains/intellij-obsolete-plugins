package com.intellij.aidebugger.python

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object AiDebuggerPythonBundle {
  private const val BUNDLE = "messages.AiDebuggerPythonBundle"

  private val INSTANCE: DynamicBundle = DynamicBundle(AiDebuggerPythonBundle::class.java, BUNDLE)

  @Nls
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return INSTANCE.getMessage(key, *params)
  }
}