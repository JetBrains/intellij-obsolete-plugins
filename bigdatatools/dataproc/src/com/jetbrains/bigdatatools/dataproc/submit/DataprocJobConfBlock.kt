package com.jetbrains.bigdatatools.dataproc.submit

import com.google.cloud.dataproc.v1.Job
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import javax.swing.JComponent

abstract class DataprocJobConfBlock : Disposable {
  protected var block: MigBlock? = null
  private var isVisible: Boolean = false

  override fun dispose() {}

  open fun setVisible(value: Boolean) {
    isVisible = value
    block?.isVisible = value

    getPropertiesComponent().isVisible = value
  }

  abstract fun initMainComponent(migPanel: MigPanel)
  abstract fun getPropertiesComponent(): JComponent
  abstract fun applyToJobBuilder(builder: Job.Builder)

  abstract fun fillByJob(jobInfo: DataprocJobInfo)
}