package com.jetbrains.spark.submit.run.local.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.run.common.ui.TextFieldFileSelector

interface LocalFileSelectorContext : FileSelectorContext {
  val defaultLocalPath: String?
}

val FileSelectorContext.defaultLocalPath: String?
  get() = (this as? LocalFileSelectorContext)?.defaultLocalPath

// TODO inline
fun LocalFileSelector(
  project: Project,
  fileSelectorType: FileSelectorType,
  fileTypeSerializer: FilePathSerializer,
  @NlsContexts.DialogTitle dialogTitle: String,
  defaultLocalPath: String? = null
) = TextFieldFileSelector(fileSelectorType, fileTypeSerializer, LocalFileSelectorContextImpl(project, dialogTitle, defaultLocalPath))