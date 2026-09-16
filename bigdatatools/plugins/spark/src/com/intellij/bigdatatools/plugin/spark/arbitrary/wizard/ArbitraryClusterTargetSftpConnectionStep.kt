package com.intellij.bigdatatools.plugin.spark.arbitrary.wizard

import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.plugin.spark.arbitrary.utils.ArbitraryClusterUtils
import com.intellij.execution.target.TargetEnvironmentWizardStepKt
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.jetbrains.bigdatatools.common.settings.defaultui.TestConnectionPanelWrapper
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionData
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionGroup
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionTesting
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JScrollPane

class ArbitraryClusterTargetSftpConnectionStep(
  val model: ArbitraryClusterTargetEnvConfiguration
) : TargetEnvironmentWizardStepKt(SparkMessagesBundle.message("arbitrary.cluster.wizard.select.sftp.step.title")) {
  private lateinit var defaultButton: Cell<JBRadioButton>
  private lateinit var customButton: Cell<JBRadioButton>
  private lateinit var noneButton: Cell<JBRadioButton>

  init {
    stepDescription = formatStepLabel(3, 3, SparkMessagesBundle.message("arbitrary.cluster.wizard.select.sftp.step.desc"))
  }

  override fun doCommit(commitType: CommitType?) {
    model.sftpSettingsCustomizer.getDefaultFields().forEach {
      it.apply(model.sftpConnectionData)
    }

    model.sftpSettingsCustomizer.getAdditionalFields().forEach {
      it.apply(model.sftpConnectionData)
    }
  }

  override fun getPreviousStepId() = ArbitraryClusterTargetSparkConnectionStep.ID

  override fun getStepId(): Any = ID

  override fun getNextStepId(): Any? = null

  override fun createMainPanel(): JComponent = panel {
    buttonsGroup {
      row {
        defaultButton = radioButton(SparkMessagesBundle.message("arbitrary.cluster.wizard.default.radio.button"),
                                    DependConnectionType.DEFAULT).onChanged {
          model.selectedSftpType = DependConnectionType.DEFAULT
        }
      }.bottomGap(BottomGap.SMALL)
      row {
        customButton = radioButton(SparkMessagesBundle.message("arbitrary.cluster.wizard.sftp.custom.radio.button"),
                                   DependConnectionType.CUSTOM).onChanged {
          model.selectedSftpType = DependConnectionType.CUSTOM
        }
      }.bottomGap(BottomGap.SMALL)
      row {
        noneButton = radioButton(SparkMessagesBundle.message("arbitrary.cluster.wizard.sftp.none.radio.button"),
                                 DependConnectionType.NONE).onChanged {
          model.selectedSftpType = DependConnectionType.NONE
        }
      }.bottomGap(BottomGap.SMALL)
    }.bind(model::selectedSftpType)
    separator()

    rowsRange {
      row(MessagesBundle.message("settings.tunnel.ssh")) {
        label(MessagesBundle.message("combobox.item.is.not.selected")).bindText(model.selectedSshConfigLabel)
      }
    }.visibleIf(defaultButton.selected)

    val sftpComponent = model.sftpSettingsCustomizer.getDefaultComponent(emptyList(), model.sftpConnectionData)

    block(JScrollPane(sftpComponent).apply {
      border = BorderFactory.createEmptyBorder()
      preferredSize = sftpComponent.preferredSize
    }).visibleIf(customButton.selected).resizableRow()

    val testConn = TestConnectionPanelWrapper(SftpConnectionTesting(model.project, model.sftpSettingsCustomizer, model.sftpConnectionData),
                                              model.sftpSettingsCustomizer.getDefaultFields(),
                                              ::createTestConnection,
                                              model.disposable, model.coroutineScope)
    row {
      cell(testConn.getMainComponent()).visibleIf(noneButton.selected.not())
    }
  }

  override fun isComplete(): Boolean = true

  override fun getPreferredFocusedComponent(): JComponent = customButton.component

  private fun createTestConnection(): SftpConnectionData {
    val connData = when (model.selectedSftpType) {
      DependConnectionType.DEFAULT -> ArbitraryClusterUtils.createSftp(model.connectionData)
      DependConnectionType.CUSTOM -> {
        val testData = SftpConnectionGroup().createBlankData()
        model.sftpSettingsCustomizer.getDefaultFields().forEach {
          it.apply(testData)
        }
        testData
      }
      DependConnectionType.NONE -> error("Is not possible")
    }

    return connData
  }

  companion object {
    @JvmStatic
    val ID: Any = ArbitraryClusterTargetSftpConnectionStep::class
  }
}