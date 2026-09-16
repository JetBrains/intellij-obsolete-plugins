package com.intellij.bigdatatools.plugin.spark.python.submit.wizard

import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.jetbrains.bigdatatools.wizard.SparkProjectType
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import com.jetbrains.python.newProjectWizard.PyV3ProjectTypeSpecificUI
import com.jetbrains.python.newProjectWizard.projectPath.ProjectPathProvider

object PyV3SparkUI : PyV3ProjectTypeSpecificUI<PyV3SparkSettings> {
  override val advancedSettings: (Panel.(PyV3SparkSettings, ProjectPathProvider) -> Unit)? = { settings, _ ->
    row(WizardMessageBundle.message("bdt.wizard.type.label")) {
      comboBox(SparkProjectType.entries, textListCellRenderer { it?.title }).bindItem(settings::projectType.toNullableProperty())
    }
    row {
      comment(WizardMessageBundle.message("wizard.package.will.be.installed"))
    }
  }
}