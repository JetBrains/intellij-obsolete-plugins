package com.intellij.aiplayground.ui

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object AIPlaygroundUIBundle {

  private const val BUNDLE: String = "messages.AIPlaygroundUIBundle"
  private val INSTANCE: DynamicBundle = DynamicBundle(AIPlaygroundUIBundle::class.java, BUNDLE)

  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
    INSTANCE.getMessage(key, *params)
}
