@file:Suppress("DialogTitleCapitalization")

package com.intellij.dbt.run

import com.intellij.application.options.ModulesComboBox
import com.intellij.dbt.DbtBundle
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.ide.wizard.setMinimumWidthForAllRowLabels
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField


class DbtRunConfigurationEditor : SettingsEditor<DbtRunConfiguration>() {
  private var myPanel: JPanel
  private var dbtCommand: ComboBox<DbtCommand> = ComboBox()
  private var dbtAdditionalArguments: JTextField = JTextField()
  private val modulesComboBox = ModulesComboBox()
  private val environmentVariablesComponent = EnvironmentVariablesTextFieldWithBrowseButton()

  init {
    DbtCommand.entries.forEach {
      dbtCommand.addItem(it)
    }

    myPanel = panel {
      row(DbtBundle.message("dbt.commands.title")) {
        cell(dbtCommand).columns(COLUMNS_MEDIUM)
      }
      row(DbtBundle.message("dbt.additional.arguments.title")) {
        cell(dbtAdditionalArguments).columns(COLUMNS_MEDIUM)
      }
      row(DbtBundle.message("dbt.run.configuration.module.title")) {
        cell(modulesComboBox).columns(COLUMNS_MEDIUM)
      }
      row(DbtBundle.message("dbt.run.configuration.env.vars.title")) {
        cell(environmentVariablesComponent).align(Align.FILL)
      }
    }.also {
      it.setMinimumWidthForAllRowLabels(JBUI.scale(120))
    }
  }

  override fun resetEditorFrom(runConfiguration: DbtRunConfiguration) {
    val modules = ModuleManager.getInstance(runConfiguration.project).modules.toList()
    modulesComboBox.setModules(modules)
    dbtCommand.item = runConfiguration.getDbtOption().dbtCommand ?: DbtCommand.RUN
    dbtAdditionalArguments.text = runConfiguration.getDbtOption().dbtAdditionalArguments.joinToString(" ")
    modulesComboBox.selectedModule = runConfiguration.configurationModule.module
    environmentVariablesComponent.envs = runConfiguration.getDbtOption().envVars
  }

  override fun applyEditorTo(runConfiguration: DbtRunConfiguration) {
    runConfiguration.setDbtCommand(dbtCommand.item)
    runConfiguration.setDbtArguments(dbtAdditionalArguments.text.split(" ").filter { it.isNotBlank() })
    runConfiguration.getDbtOption().envVars = environmentVariablesComponent.envs
    runConfiguration.configurationModule.module = modulesComboBox.selectedModule
  }

  override fun createEditor(): JComponent = myPanel
}