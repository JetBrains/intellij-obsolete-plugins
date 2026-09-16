package com.jetbrains.bigdatatools.dataproc.submit

import com.google.cloud.dataproc.v1.Job
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.common.ui.components.TagsComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileTypeSerializer
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocUiComponents
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.run.common.ui.FileMultiSelector
import com.jetbrains.spark.submit.run.common.ui.row

class DataprocHadoopJobConfBlock(val project: Project,
                                 val dataManager: DataprocDataManager,
                                 driversProvider: DataprocDriversProvider,
                                 clusterNameProvider: () -> String?) : DataprocJobConfBlock() {
  private val jarFilesField = DataprocUiComponents.createJarsField(project, this, driversProvider)

  private val mainClassField = DataprocUiComponents.createMainClassField(project, this, dataManager,
                                                                         clusterNameProvider = clusterNameProvider,
                                                                         pathProvider = { jarFilesField.files.firstOrNull() })
  private val archiveFilesField = DataprocUiComponents.createArchivesField(project, this, driversProvider)
  private val arguments = DataprocUiComponents.createArgumentsField()
  private val properties = TagsComponent(emptyList())

  override fun fillByJob(jobInfo: DataprocJobInfo) {
    val job = jobInfo.job.hadoopJob
    mainClassField.childComponent.text = job.mainClass
    jarFilesField.files = job.jarFileUrisList.map { DataprocFileTypeSerializer.fromText(it) }
    archiveFilesField.files = job.archiveUrisList.map { DataprocFileTypeSerializer.fromText(it) }
    arguments.text = job.argsList.joinToString(separator = " ") { it }
    properties.updateExternally(job.propertiesMap)
  }


  override fun initMainComponent(migPanel: MigPanel) {
    block = MigBlock(migPanel).apply {
      row(jarFilesField)
      row(DataprocMessagesBundle.message("job.info.spark.main.class"), mainClassField)
      row(archiveFilesField as FileMultiSelector)
      row(DataprocMessagesBundle.message("job.info.spark.args"), arguments)
    }
    block?.isVisible = false
  }

  override fun getPropertiesComponent() = properties.component

  override fun applyToJobBuilder(builder: Job.Builder) {
    val jobBuilder = builder.hadoopJobBuilder

    jobBuilder.addAllJarFileUris(jarFilesField.files.map { DataprocFileTypeSerializer.toText(it) })
    jobBuilder.addAllArchiveUris(archiveFilesField.files.map { DataprocFileTypeSerializer.toText(it) })
    jobBuilder.addArgs(arguments.text)

    jobBuilder.mainClass = mainClassField.childComponent.text

    jobBuilder.putAllProperties(properties.getValueAsMap())
  }
}