package com.intellij.bigdatatools.plugin.spark.arbitrary.wizard

import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.plugin.spark.arbitrary.utils.ArbitraryClusterUtils
import com.intellij.execution.target.TargetEnvironmentWizardStepKt
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.bigdatatools.common.settings.defaultui.TestConnectionPanelWrapper
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.settings.SparkConnectionGroup
import com.jetbrains.spark.monitoring.settings.SparkConnectionTestingBase
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.plus
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JScrollPane

class ArbitraryClusterTargetSparkConnectionStep(
  val model: ArbitraryClusterTargetEnvConfiguration
) : TargetEnvironmentWizardStepKt(SparkMessagesBundle.message("arbitrary.cluster.wizard.select.spark.step.title")) {
  private lateinit var defaultButton: Cell<JBRadioButton>
  private lateinit var customButton: Cell<JBRadioButton>
  private lateinit var noneButton: Cell<JBRadioButton>

  init {
    stepDescription = formatStepLabel(2, 3, SparkMessagesBundle.message("arbitrary.cluster.wizard.select.spark.step.desc"))
  }

  override fun doCommit(commitType: CommitType?) {
    model.sparkSettingsCustomizer.getDefaultFields().forEach {
      it.apply(model.sparkConnectionData)
    }

    model.sparkSettingsCustomizer.getAdditionalFields().forEach {
      it.apply(model.sparkConnectionData)
    }
  }

  override fun getPreviousStepId() = ArbitraryClusterTargetSshConnectionStep.ID

  override fun getStepId() = ID

  override fun getNextStepId() = ArbitraryClusterTargetSftpConnectionStep.ID

  override fun createMainPanel(): JComponent = panel {
    buttonsGroup {
      row {
        defaultButton = radioButton(SparkMessagesBundle.message("arbitrary.cluster.wizard.default.radio.button"),
                                    DependConnectionType.DEFAULT).onChanged {
          model.selectedSparkType = DependConnectionType.DEFAULT
        }
      }.bottomGap(BottomGap.SMALL)
      row {
        customButton = radioButton(SparkMessagesBundle.message("arbitrary.cluster.wizard.spark.custom.radio.button"),
                                   DependConnectionType.CUSTOM).onChanged {
          model.selectedSparkType = DependConnectionType.CUSTOM
        }

      }.bottomGap(BottomGap.SMALL)
      row {
        noneButton = radioButton(SparkMessagesBundle.message("arbitrary.cluster.wizard.spark.none.radio.button"),
                                 DependConnectionType.NONE).onChanged {
          model.selectedSparkType = DependConnectionType.NONE
        }
      }.bottomGap(BottomGap.SMALL)
    }.bind(model::selectedSparkType)
    separator()

    rowsRange {
      row(MessagesBundle.message("settings.url")) {
        label(ArbitraryClusterUtils.DEFAULT_SPARK_URL)
        comment(SparkMessagesBundle.message("arbitrary.cluster.wizard.spark.step.default.uri.comment.text"))
      }
      row(MessagesBundle.message("settings.tunnel.ssh")) {
        label(MessagesBundle.message("combobox.item.is.not.selected")).bindText(model.selectedSshConfigLabel)
      }
    }.visibleIf(defaultButton.selected)

    val sparkSettingsComponent = model.sparkSettingsCustomizer.getDefaultComponent(emptyList(), model.sparkConnectionData)

    block(JScrollPane(sparkSettingsComponent).apply {
      border = BorderFactory.createEmptyBorder()
      preferredSize = sparkSettingsComponent.preferredSize
    }).visibleIf(customButton.selected).resizableRow()

    val scope = SafeExecutor.createInstance(model.disposable).coroutineScope.plus(
      ModalityState.any().asContextElement())
    val testConn = TestConnectionPanelWrapper(SparkConnectionTestingBase(model.project, model.sparkSettingsCustomizer),
                                              model.sparkSettingsCustomizer.getDefaultFields(),
                                              ::createTestConnection,
                                              model.disposable, scope)

    row {
      cell(testConn.getMainComponent()).visibleIf(noneButton.selected.not())
    }
  }

  override fun isComplete(): Boolean = true

  override fun getPreferredFocusedComponent(): JComponent = customButton.component

  private fun createTestConnection(): SparkConnectionData {
    val connData = when (model.selectedSparkType) {
      DependConnectionType.DEFAULT -> ArbitraryClusterUtils.createSparkConnection(model.connectionData)
      DependConnectionType.CUSTOM -> {
        val testData = SparkConnectionGroup().createBlankData()
        model.sparkSettingsCustomizer.getDefaultFields().forEach {
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
    val ID: Any = ArbitraryClusterTargetSparkConnectionStep::class
  }
}