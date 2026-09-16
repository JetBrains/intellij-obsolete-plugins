package com.jetbrains.spark.submit.run.cluster.ui

import com.intellij.ui.TextAccessor
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.DslComponentProperty
import com.intellij.ui.dsl.gridLayout.toUnscaledGaps
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.SchemeFilePathSerializer
import com.jetbrains.spark.submit.run.common.ui.FileSelectorContextImpl
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import javax.swing.text.JTextComponent

class ExtendableTextFieldFileSelector(
  fileSelectorType: FileSelectorType,
  filePathSerializer: FilePathSerializer = SchemeFilePathSerializer,
  fileSelectorContext: FileSelectorContextImpl,
  defaultText: String = "",
) : FileSelector(filePathSerializer, fileSelectorContext) {
  override val component: ExtendableTextField = ExtendableTextField().also { textField ->
    textField.emptyText.text = defaultText
    val fileSelectorOptions = FileSelectorOption.getForType(fileSelectorType)
    fileSelectorOptions.forEach { fileTypeAction ->
      if (fileTypeAction.isAvailable(fileSelectorContext.project)) {
        textField.addExtension(ExtendableTextComponent.Extension.create(fileTypeAction.icon, fileTypeAction.tooltip) {
          selectFile(fileTypeAction)
        })
      }
    }
    textField.putClientProperty(DslComponentProperty.VISUAL_PADDINGS, textField.insets.toUnscaledGaps())
  }

  override val textAccessor: TextAccessor get() = component
  override val interactiveComponent: JTextComponent get() = component

}

