package com.jetbrains.bigdatatools.dataproc.ui.component

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.spark.submit.run.common.ui.FileMultiSelector
import com.jetbrains.spark.submit.run.common.ui.FileSelectorContextImpl

// TODO inline
fun DataprocFileMultiSelector(
  @DialogTitle title: String,
  @Label label: String,
  project: Project,
  driversProvider: DataprocDriversProvider
) = FileMultiSelector(DataprocFileSelectorUtils.DATAPROC_UPLOAD, DataprocFileTypeSerializer, label, DataprocFileSelectorContextImpl(project, title, driversProvider))

class DataprocFileSelectorContextImpl(
  project: Project,
  @DialogTitle dialogTitle: String,
  override val driversProvider: DataprocDriversProvider
) : FileSelectorContextImpl(project, dialogTitle), DataprocFileSelectorContext {
  override val hasBeforeTasks: Boolean get() = false
}