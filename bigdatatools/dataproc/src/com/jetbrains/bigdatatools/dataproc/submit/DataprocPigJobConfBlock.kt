package com.jetbrains.bigdatatools.dataproc.submit

import com.google.cloud.dataproc.v1.Job
import com.google.cloud.dataproc.v1.PigJob
import com.google.cloud.dataproc.v1.QueryList
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileTypeSerializer
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.run.common.ui.row

class DataprocPigJobConfBlock(project: Project,
                              dataManager: DataprocDataManager,
                              driversProvider: DataprocDriversProvider) : DataprocAbstractQuerylJobConfBlock(project,
                                                                                                             dataManager,
                                                                                                             driversProvider) {
  override fun fillByJob(jobInfo: DataprocJobInfo) {
    val job = jobInfo.job.pigJob
    queryTypeField.isSelected = job.queriesCase == PigJob.QueriesCase.QUERY_FILE_URI
    queryFile.path = DataprocFileTypeSerializer.fromText(job.queryFileUri)
    queryText.text = job.queryList.queriesList.joinToString(separator = "\n") { it }
    jarFilesField.files = job.jarFileUrisList.map { DataprocFileTypeSerializer.fromText(it) }
    continueOnFailure.isSelected = job.continueOnFailure

    properties.updateExternally(job.propertiesMap)
  }

  override fun initMainComponent(migPanel: MigPanel) {
    block = MigBlock(migPanel).apply {
      row(DataprocMessagesBundle.message("job.query.source.type"), queryTypeField.getComponent())
      row(queryFileLabel, queryFile.component)
      row(queryTextLabel, queryText)

      row(jarFilesField)
      row(continueOnFailure)
    }
    block?.isVisible = false
  }

  override fun applyToJobBuilder(builder: Job.Builder) {
    val jobBuilder = builder.pigJobBuilder
    if (queryTypeField.isSelected)
      jobBuilder.queryFileUri = DataprocFileTypeSerializer.toText(queryFile.path)
    else
      jobBuilder.setQueryList(QueryList.newBuilder().addQueries(queryText.text))

    jobBuilder.addAllJarFileUris(jarFilesField.files.map { DataprocFileTypeSerializer.toText(it) })
    jobBuilder.continueOnFailure = continueOnFailure.isSelected

    jobBuilder.putAllProperties(properties.getValueAsMap())
  }
}