package com.jetbrains.spark.submit.run.ui

import com.intellij.ui.components.fields.IntegerField

class EmptyIntegerField : IntegerField() {
  override fun validateContent() {
    if (text.isBlank())
      return
    super.validateContent()
  }
}