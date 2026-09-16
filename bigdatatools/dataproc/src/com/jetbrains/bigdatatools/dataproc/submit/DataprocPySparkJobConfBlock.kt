package com.jetbrains.bigdatatools.dataproc.submit

import com.google.cloud.dataproc.v1.Job
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.common.ui.components.TagsComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileMultiSelector
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileSelector
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileTypeSerializer
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileSelectorUtils.withDataprocFsValidator
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocUiComponents
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.run.common.ui.row
import com.jetbrains.spark.submit.run.common.ui.withNonEmptyValidator

class DataprocPySparkJobConfBlock(val project: Project,
                                  val dataManager: DataprocDataManager,
                                  driversProvider: DataprocDriversProvider) : DataprocJobConfBlock() {
  private val mainPythonFile = DataprocFileSelector(DataprocMessagesBundle.message("job.info.spark.main.py.file.title"),
                                                    project,
                                                    driversProvider)
    .withNonEmptyValidator(this)
    .also {
      it.toolTipText = DataprocMessagesBundle.message("job.info.single.file.hint")
    }

  private val additionalPyFiles = DataprocFileMultiSelector(DataprocMessagesBundle.message("job.info.spark.additional.py.files.title"),
                                                            DataprocMessagesBundle.message("job.info.spark.additional.py.files"),
                                                            project,
                                                            driversProvider)
    .withDataprocFsValidator(this)


  private val jarFilesField = DataprocUiComponents.createJarsField(project, this, driversProvider)
  private val archiveFilesField = DataprocUiComponents.createArchivesField(project, this, driversProvider)
  private val arguments = DataprocUiComponents.createArgumentsField()
  private val properties = TagsComponent(emptyList())

  override fun fillByJob(jobInfo: DataprocJobInfo) {
    val job = jobInfo.job.pysparkJob
    mainPythonFile.path = DataprocFileTypeSerializer.fromText(job.mainPythonFileUri)
    additionalPyFiles.files = job.pythonFileUrisList.map { DataprocFileTypeSerializer.fromText(it) }
    jarFilesField.files = job.jarFileUrisList.map { DataprocFileTypeSerializer.fromText(it) }
    archiveFilesField.files = job.archiveUrisList.map { DataprocFileTypeSerializer.fromText(it) }
    arguments.text = job.argsList.joinToString(separator = " ") { it }
    properties.updateExternally(job.propertiesMap)
  }


  override fun initMainComponent(migPanel: MigPanel) {
    block = MigBlock(migPanel).apply {
      row(DataprocMessagesBundle.message("job.info.spark.main.pyfile"), mainPythonFile.component)
      row(additionalPyFiles)
      row(jarFilesField)
      row(archiveFilesField)
      row(DataprocMessagesBundle.message("job.info.spark.args"), arguments)
    }
    block?.isVisible = false
  }

  override fun getPropertiesComponent() = properties.component

  override fun applyToJobBuilder(builder: Job.Builder) {
    val jobBuilder = builder.pysparkJobBuilder
    jobBuilder.mainPythonFileUri = DataprocFileTypeSerializer.toText(mainPythonFile.path)
    jobBuilder.addAllPythonFileUris(additionalPyFiles.files.map { DataprocFileTypeSerializer.toText(it) })
    jobBuilder.addAllJarFileUris(jarFilesField.files.map { DataprocFileTypeSerializer.toText(it) })
    jobBuilder.addAllArchiveUris(archiveFilesField.files.map { DataprocFileTypeSerializer.toText(it) })
    jobBuilder.addArgs(arguments.text)

    jobBuilder.putAllProperties(properties.getValueAsMap())
  }
}