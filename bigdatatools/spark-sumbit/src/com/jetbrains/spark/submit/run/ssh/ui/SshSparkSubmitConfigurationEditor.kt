package com.jetbrains.spark.submit.run.ssh.ui

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.remote.PresentableId
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshConfigComboBox
import com.intellij.ssh.ui.unified.SshConfigVisibility
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.bigdatatools.coreUi.ui.row
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.SchemeFilePathSerializer
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import com.jetbrains.spark.submit.run.ssh.SshSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.SshSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent

class SshSparkSubmitConfigurationEditor(
  project: Project,
  val factory: SshSparkSubmitConfigurationFactory
) : SshAwareSparkSubmitConfigurationEditor<SshSparkJobRunConfiguration>(project) {

  private val sshVisibility = if (project.isDefault) SshConfigVisibility.App else SshConfigVisibility.Project

  private val sshComboBox = SshConfigComboBox(project, this, sshVisibility)

  override suspend fun getSshConfig(): SshConfig? {
    return withContext(Dispatchers.EDT) {
      sshComboBox.selectedSshConfig
    }
  }

  override val artifactPathField: FileSelector = SshFileSelector(SparkMessagesBundle.message("dialog.artifactPath.title"),
                                                                 project,
                                                                 FileSelectorType.SSH_JAR,
                                                                 SchemeFilePathSerializer) {
    getSshConfig()
  }

  override fun createEditor(): JComponent {
    val panel = panel {
      finalCommandRow()

      row(SparkMessagesBundle.message("settings.ssh.config"), sshComboBox)
      row(SparkMessagesBundle.message("settings.ssh.target.dir"), targetDirectory.component)

      row { cell(optionsLink).align(AlignX.RIGHT) }
      mainSparkSettingsRow()
      optionBlocks()
      updateFieldsForManagerType()
    }

    return panel
  }

  override fun createTemporaryConfiguration() =
    factory.createTemplateConfiguration(project)

  override fun resetEditorFrom(s: SshSparkJobRunConfiguration) {
    val sshConfig = SshConfigManager.getInstance(project).findConfigById(s.sshConfigId)
    sshComboBox.reload(PresentableId.createId(sshConfig?.id, null), SshConfigVisibility.Project)
    initSparkMonitoringComboBox(s.sparkMonitoringDriverId)
    super.resetEditorFrom(s)
  }

  override fun applyEditorTo(s: SshSparkJobRunConfiguration) {
    s.sshConfigId = sshComboBox.selectedSshConfig?.id ?: ""
    s.sparkMonitoringDriverId = sparkMonitoringId()
    super.applyEditorTo(s)
  }
}