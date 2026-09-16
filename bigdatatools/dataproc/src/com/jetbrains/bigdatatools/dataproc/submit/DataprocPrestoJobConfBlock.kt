package com.jetbrains.bigdatatools.dataproc.submit

import com.google.cloud.dataproc.v1.Job
import com.google.cloud.dataproc.v1.PrestoJob
import com.google.cloud.dataproc.v1.QueryList
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileTypeSerializer
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle

class DataprocPrestoJobConfBlock(project: Project,
                                 dataManager: DataprocDataManager,
                                 driversProvider: DataprocDriversProvider) : DataprocAbstractQuerylJobConfBlock(project,
                                                                                                                dataManager,
                                                                                                                driversProvider) {
  override fun fillByJob(jobInfo: DataprocJobInfo) {
    val job = jobInfo.job.prestoJob
    queryTypeField.isSelected = job.queriesCase == PrestoJob.QueriesCase.QUERY_FILE_URI
    queryFile.path = DataprocFileTypeSerializer.fromText(job.queryFileUri)
    queryText.text = job.queryList.queriesList.joinToString(separator = "\n") { it }
    continueOnFailure.isSelected = job.continueOnFailure

    properties.updateExternally(job.propertiesMap)
  }

  override fun initMainComponent(migPanel: MigPanel) {
    block = MigBlock(migPanel).apply {
      row(DataprocMessagesBundle.message("job.query.source.type"), queryTypeField.getComponent())
      row(queryFileLabel, queryFile.component)
      row(queryTextLabel, queryText)

      row(continueOnFailure)
    }
    block?.isVisible = false
  }

  override fun applyToJobBuilder(builder: Job.Builder) {
    val jobBuilder = builder.prestoJobBuilder
    if (queryTypeField.isSelected)
      jobBuilder.queryFileUri = DataprocFileTypeSerializer.toText(queryFile.path)
    else
      jobBuilder.setQueryList(QueryList.newBuilder().addQueries(queryText.text))

    jobBuilder.continueOnFailure = continueOnFailure.isSelected

    jobBuilder.putAllProperties(properties.getValueAsMap())
  }
}