package com.jetbrains.bigdatatools.dataproc.ui.component

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.run.common.ui.TextFieldFileSelector

interface DataprocFileSelectorContext : FileSelectorContext {
  val driversProvider: DataprocDriversProvider
}

// TODO inline
fun DataprocFileSelector(
  @DialogTitle title: String,
  project: Project,
  driversProvider: DataprocDriversProvider
) = TextFieldFileSelector(DataprocFileSelectorUtils.DATAPROC_UPLOAD, DataprocFileTypeSerializer, DataprocFileSelectorContextImpl(project, title, driversProvider), defaultText = "")