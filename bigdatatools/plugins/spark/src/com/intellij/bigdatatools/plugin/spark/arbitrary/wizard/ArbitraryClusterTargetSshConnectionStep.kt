package com.intellij.bigdatatools.plugin.spark.arbitrary.wizard

import com.intellij.execution.target.TargetEnvironmentWizardStepKt
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.bind
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.editableValueMatches
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.ui.bindIcon
import com.jetbrains.bigdatatools.sftp.settings.fields.SshConfigWrappedComponent
import com.jetbrains.bigdatatools.sftp.util.SftpMessagesBundle
import com.jetbrains.spark.submit.checker.SshConnectionChecker
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon
import javax.swing.JComponent

internal class ArbitraryClusterTargetSshConnectionStep(
  val model: ArbitraryClusterTargetEnvConfiguration
) : TargetEnvironmentWizardStepKt(SparkMessagesBundle.message("arbitrary.cluster.wizard.select.ssh.step.title")) {
  private val sshComponent = SshConfigWrappedComponent(ModificationKey(SftpMessagesBundle.message("settings.fields.ssh")),
                                                       model.connectionData, model.project, this)

  private val statusText = AtomicProperty("")
  private val statusIcon = AtomicProperty<Icon?>(null)
  private var sshConnectionChecker: SshConnectionChecker? = null

  init {
    stepDescription = formatStepLabel(1, 3, SparkMessagesBundle.message("arbitrary.cluster.wizard.select.ssh.step.desc"))
  }

  override fun doCommit(commitType: CommitType?) {
    model.selectedSshConfig.set(sshComponent.getConfig())
    model.connectionData.sshId = sshComponent.getConfig()?.id ?: ""
    sshComponent.getComponent().selectedSshConfig?.let {
      model.sparkSettingsCustomizer.tunnelField.setSshConnection(it)
    }
    sshComponent.getComponent().selectedSshConfig?.let {
      model.sftpSettingsCustomizer.sshComponent.setValue(it)
    }
  }

  override fun getStepId(): Any = ID

  override fun getNextStepId() = ArbitraryClusterTargetSparkConnectionStep.ID

  override fun getPreviousStepId(): Any? = null

  override fun createMainPanel(): JComponent = panel {
    row(MessagesBundle.message("settings.tunnel.ssh"), sshComponent.getComponent())
    row {
      label("").applyToComponent {
        bind(statusText)
        bindIcon(statusIcon)
      }.visibleIf(sshComponent.getComponent().comboBox.editableValueMatches { it != null })
    }

    sshComponent.getComponent().setDataListener {
      checkConnection()
      fireStateChanged()
    }
  }

  override fun isComplete(): Boolean = sshComponent.getComponent().selectedSshConfig != null

  override fun getPreferredFocusedComponent(): JComponent = sshComponent.getComponent()

  private fun checkConnection() {
    sshConnectionChecker?.cancel()
    sshConnectionChecker = object : SshConnectionChecker(model.coroutineScope, model.project) {
      override suspend fun getSshConfig(): SshConfig? = sshComponent.getComponent().selectedSshConfig
      override fun updateStatusText(text: String) = statusText.set(text)
      override fun updateStatusIcon(icon: Icon?) = statusIcon.set(icon)
      override fun updateLinkText(linkText: String) {}
    }
    sshConnectionChecker?.invoke()
  }

  companion object {
    @JvmStatic
    val ID: Any = ArbitraryClusterTargetSshConnectionStep::class
  }
}