package com.jetbrains.bigdatatools.dataproc.submit

import com.intellij.openapi.project.Project
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBLabel
import com.jetbrains.bigdatatools.common.ui.components.RadioCheckBox
import com.jetbrains.bigdatatools.common.ui.components.TagsComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileSelector
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocUiComponents
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.run.common.ui.withNonEmptyValidator

abstract class DataprocAbstractQuerylJobConfBlock(val project: Project,
                                                  val dataManager: DataprocDataManager,
                                                  driversProvider: DataprocDriversProvider) : DataprocJobConfBlock() {
  protected val queryFileLabel = JBLabel(DataprocMessagesBundle.message("job.query.file.label"))
  protected val queryFile = DataprocFileSelector(DataprocMessagesBundle.message("job.query.file.dialog.title"),
                                                 project,
                                                 driversProvider)
    .withNonEmptyValidator(this)
    .also {
      it.toolTipText = DataprocMessagesBundle.message("job.info.single.file.hint")
    }

  protected val queryTextLabel = JBLabel(DataprocMessagesBundle.message("job.query.text.label"))
  protected val queryText = RawCommandLineEditor().also {
    it.textField.toolTipText = DataprocMessagesBundle.message("job.query.text.hint")
  }

  protected val queryTypeField = RadioCheckBox(
    DataprocMessagesBundle.message("job.query.source.file"),
    DataprocMessagesBundle.message("job.query.source.text")).apply {
    addItemListener { onQueryTypeChange() }
  }

  protected val continueOnFailure = CheckBox(DataprocMessagesBundle.message("job.info.continue.on.failure"))

  @Suppress("LeakingThis")
  protected val jarFilesField = DataprocUiComponents.createJarsField(project, this, driversProvider)

  protected val properties = TagsComponent(emptyList())

  init {
    queryTypeField.isSelected = false
    onQueryTypeChange()
  }

  override fun getPropertiesComponent() = properties.component

  override fun setVisible(value: Boolean) {
    super.setVisible(value)
    if (value)
      onQueryTypeChange()
  }

  private fun onQueryTypeChange() {
    val isFileSelected = queryTypeField.isSelected

    queryFile.component.isVisible = isFileSelected
    queryFileLabel.isVisible = isFileSelected

    queryText.isVisible = !isFileSelected
    queryTextLabel.isVisible = !isFileSelected
  }
}