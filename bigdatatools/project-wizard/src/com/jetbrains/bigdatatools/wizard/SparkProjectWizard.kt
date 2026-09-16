package com.jetbrains.bigdatatools.wizard

import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.wizard.newProjectWizardBaseStepWithoutGap
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import javax.swing.Icon

class SparkProjectWizard : GeneratorNewProjectWizard {
  override val id: String
    get() = "bdt.template.new.project.generator"
  override val name: String
    get() = WizardMessageBundle.message("bdt.wizard.spark.template.name")
  override val icon: Icon
    get() = myIcon

  override fun createStep(context: WizardContext): NewProjectWizardStep {
    return RootNewProjectWizardStep(context)
      .nextStep(::newProjectWizardBaseStepWithoutGap)
      .nextStep(::SparkProjectWizardStep)
  }

  companion object {
    private val myIcon: Icon = BigdatatoolsSparkMonitoringIcons.Spark
  }
}

class BdtNewProjectWizardAdapter : GeneratorNewProjectWizardBuilderAdapter(SparkProjectWizard()) {
  override fun isAvailable(): Boolean =
    super.isAvailable() && BuildSystemBasedStructureProviderFactory.getAll().isNotEmpty() && SparkProjectLanguageHandler.getAll().isNotEmpty()
}