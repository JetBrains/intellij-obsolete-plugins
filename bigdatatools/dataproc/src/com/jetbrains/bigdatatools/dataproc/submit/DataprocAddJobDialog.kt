package com.jetbrains.bigdatatools.dataproc.submit

import com.google.cloud.dataproc.v1.Job
import com.google.cloud.dataproc.v1.OrderedJob
import com.google.cloud.dataproc.v1.OrderedJob.JobTypeCase
import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.bigdatatools.coreUi.settings.withNonEmptyValidator
import com.intellij.bigdatatools.coreUi.settings.withNumberOrEmptyValidator
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.ui.components.TagsComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocUiComponents
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocUiComponents.title
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.dataproc.util.GcRegion
import java.util.UUID
import javax.swing.JComponent

class DataprocAddJobDialog(project: Project,
                           dataManager: DataprocDataManager,
                           clusterName: String?) : DialogWrapper(project) {
  private val driversProvider = DataprocDriversProvider(project, dataManager).also {
    Disposer.register(disposable, it)
  }

  @Suppress("HardCodedStringLiteral")
  private val jobId = JBTextField("job-bdt-${UUID.randomUUID().toString().split("-").first()}")
    .withNonEmptyValidator(disposable)

  private val clusterField = DataprocUiComponents.createClusterComboBox(dataManager, clusterName)

  private val maxRestartPerHourField = JBTextField().withNumberOrEmptyValidator(disposable).also {
    it.toolTipText = DataprocMessagesBundle.message("job.info.max.restart.per.hour.hint")
    (it as JBTextField).emptyText.text = "0-10"
  }

  private val jobType = ComboBox(DataprocUiComponents.jobTypeCases).apply {
    selectedItem = GcRegion.getFromId(dataManager.region)
    renderer = CustomListCellRenderer<JobTypeCase> { it.title }
    this.addItemListener {
      updateJobBlockVisibility()
    }
  }

  private val labels = TagsComponent(emptyList())

  private val mainPanel = MigPanel()
  private val jobBlocks = DataprocUiComponents.createAddJobBlocks(project, dataManager, driversProvider, disposable) {
    clusterField.item
  }

  init {
    this.title = DataprocMessagesBundle.message("add.job.title")
    init()
    updateJobBlockVisibility()
  }

  fun initByConfig(jobInfo: DataprocJobInfo) {
    clusterField.item = jobInfo.cluster
    jobType.item = OrderedJob.JobTypeCase.valueOf(jobInfo.type)
    jobBlocks.values.forEach {
      it?.fillByJob(jobInfo)
    }

    labels.updateExternally(jobInfo.job.labelsMap)
    maxRestartPerHourField.text = jobInfo.job.scheduling.maxFailuresPerHour.toString()
  }

  override fun createCenterPanel(): JComponent = mainPanel.apply {
    row(DataprocMessagesBundle.message("job.info.jobId"), jobId)
    row(DataprocMessagesBundle.message("job.info.cluster"), clusterField)
    separatorRow()

    row(DataprocMessagesBundle.message("job.info.type"), jobType)
    jobBlocks.values.filterNotNull().forEach {
      it.initMainComponent(this)
    }
    separatorRow()

    row(DataprocMessagesBundle.message("job.info.max.restart.per.hour"), maxRestartPerHourField)

    title(DataprocMessagesBundle.message("job.properties.block.title"))
    jobBlocks.values.filterNotNull().forEach {
      block(it.getPropertiesComponent())
    }

    title(DataprocMessagesBundle.message("job.label.block.title"))
    block(labels.component)
  }

  fun getResult(): Job {
    val builder = Job.newBuilder()
    builder.referenceBuilder.jobId = jobId.text
    builder.placementBuilder.clusterName = clusterField.item

    jobBlocks[jobType.item]?.applyToJobBuilder(builder)

    maxRestartPerHourField.text.toIntOrNull()?.let {
      builder.schedulingBuilder.maxFailuresPerHour = it
    }
    builder.putAllLabels(labels.getValueAsMap())
    return builder.build()
  }

  private fun updateJobBlockVisibility() {
    jobBlocks.values.forEach { it?.setVisible(false) }
    jobBlocks[jobType.item]?.setVisible(true)
  }
}