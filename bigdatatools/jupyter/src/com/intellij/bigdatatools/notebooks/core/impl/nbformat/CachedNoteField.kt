package com.intellij.bigdatatools.notebooks.core.impl.nbformat

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import kotlin.reflect.KProperty0

class CachedNoteField<T : Any>(val note: KProperty0<BasicNotebook?>, val updater: () -> T) {
  private var timeStamp = 0L
  private lateinit var cachedValue: T

  fun getValue(): T {
    val noteTimeStamp = (note.get() as? BasicNotebookImpl)?.lastChangeTimestamp ?: error("")
    if (timeStamp != noteTimeStamp) {
      cachedValue = updater()
      timeStamp = noteTimeStamp
    }
    return cachedValue
  }
}