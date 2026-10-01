package com.intellij.dataWrangler.annotations

enum class ColumnIntent {
  CHANGE,
  REFERENCE,
  ADD,
  REMOVE,
}

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DWColumnIntent(val intent: ColumnIntent = ColumnIntent.CHANGE)
