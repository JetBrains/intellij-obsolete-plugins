package com.intellij.bigdatatools.notebooks.utils

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.NotebookBundle"

object NoteMessagesBundle {
  private val instance = DynamicBundle(NoteMessagesBundle::class.java, BUNDLE)
  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = instance.getMessage(key, *params)
}