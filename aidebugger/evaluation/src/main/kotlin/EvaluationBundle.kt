package com.intellij.aidebugger.evaluation

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object EvaluationBundle {
  private const val BUNDLE = "messages.EvaluationBundle"

  private val INSTANCE: DynamicBundle = DynamicBundle(EvaluationBundle::class.java, BUNDLE)

  @Nls
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return INSTANCE.getMessage(key, *params)
  }
}