package com.jetbrains.spark.submit.run.local.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.run.common.ui.FileMultiSelector
import com.jetbrains.spark.submit.run.common.ui.FileSelectorContextImpl

// TODO inline
fun LocalFileMultiSelector(
  project: Project,
  fileSelectorType: FileSelectorType,
  filePathSerializer: FilePathSerializer,
  @Label label: String,
  @DialogTitle dialogTitle: String,
  defaultLocalPath: String? = null
) = FileMultiSelector(fileSelectorType, filePathSerializer, label, LocalFileSelectorContextImpl(project, dialogTitle, defaultLocalPath))

class LocalFileSelectorContextImpl(
  project: Project,
  @DialogTitle dialogTitle: String,
  override val defaultLocalPath: String? = null
) : FileSelectorContextImpl(project, dialogTitle), LocalFileSelectorContext