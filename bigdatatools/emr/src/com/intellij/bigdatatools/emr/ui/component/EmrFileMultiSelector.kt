package com.intellij.bigdatatools.emr.ui.component

import com.intellij.bigdatatools.emr.dependend.drivers.EmrDriversProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.jetbrains.spark.submit.run.common.ui.FileMultiSelector
import com.jetbrains.spark.submit.run.common.ui.FileSelectorContextImpl

// TODO inline
fun EmrFileMultiSelector(
  @DialogTitle title: String,
  @Label label: String,
  project: Project,
  driversProvider: EmrDriversProvider
) = FileMultiSelector(EmrFileSelectorUtils.EMR_UPLOAD_WITHOUT_S3, EmrTypesSerializer, label, EmrFileSelectorContextImpl(project, title, driversProvider))

class EmrFileSelectorContextImpl(
  project: Project,
  @DialogTitle dialogTitle: String,
  override val driversProvider: EmrDriversProvider
) : FileSelectorContextImpl(project, dialogTitle), EmrFileSelectorContext {
  override val hasBeforeTasks: Boolean get() = false
}