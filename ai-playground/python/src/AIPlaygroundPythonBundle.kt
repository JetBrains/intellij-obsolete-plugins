package com.intellij.aiplayground.python

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object AIPlaygroundPythonBundle {

  private const val BUNDLE: String = "messages.AIPlaygroundPythonBundle"
  private val INSTANCE: DynamicBundle = DynamicBundle(AIPlaygroundPythonBundle::class.java, BUNDLE)

  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
    INSTANCE.getMessage(key, *params)
}