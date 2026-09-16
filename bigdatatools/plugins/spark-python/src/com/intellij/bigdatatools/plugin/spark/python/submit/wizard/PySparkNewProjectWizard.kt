package com.intellij.bigdatatools.plugin.spark.python.submit.wizard

import com.intellij.bigdatatools.sparkSubmit.icons.BigdatatoolsSparkSubmitIcons
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.startup.StartupManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.dsl.builder.Panel
import com.jetbrains.bigdatatools.wizard.SparkProjectType
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import com.jetbrains.python.newProject.NewProjectWizardPythonData
import com.jetbrains.python.newProject.NewPythonProjectStep
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.ui.PythonPackageManagerUI
import com.jetbrains.python.packaging.management.ui.installPackagesBackground
import com.jetbrains.python.packaging.utils.PyPackageCoroutine
import com.jetbrains.python.sdk.baseDir
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

/**
 * A PySpark project generator for IntelliJ and PyCharm (for future versions).
 */
internal class PySparkNewProjectWizard : GeneratorNewProjectWizard {
  override val id: String = "pyspark"
  override val name: String
    get() = SparkMessagesBundle.message("pyspark")
  override val icon: Icon
    get() = BigdatatoolsSparkSubmitIcons.PySpark


  override fun createStep(context: WizardContext): NewProjectWizardStep =
    RootNewProjectWizardStep(context)
      .nextStep(::NewProjectWizardBaseStep)
      .nextStep(::NewPythonProjectStep)
      .nextStep(::Step)

  class Step<T>(parent: T)
    : AbstractNewProjectWizardStep(parent),
      NewProjectWizardPythonData by parent
    where T : NewProjectWizardStep, T : NewProjectWizardPythonData {

    private val sparkTypeProperty = AtomicProperty(SparkProjectType.BATCH)

    private val requirements = listOf("pyspark")

    override fun setupUI(builder: Panel) {
      with(builder) {
        group(WizardMessageBundle.message("bdt.wizard.pyspark.group")) {
          row(WizardMessageBundle.message("bdt.wizard.type.label")) {
            segmentedButton(SparkProjectType.entries) { this.text = it.title }.bind(sparkTypeProperty)
          }
          row {
            comment(WizardMessageBundle.message("wizard.package.will.be.installed"))
          }
        }
      }
    }

    override fun setupProject(project: Project) {
      StartupManager.getInstance(project).runWhenProjectIsInitialized(object : Runnable, DumbAware {
        override fun run() {
          setupOpenProject(project)
        }
      })
    }

    private fun setupOpenProject(project: Project) {
      val sdk = pythonSdk ?: return
      installRequirements(project, sdk)
      val moduleData = module
      if (moduleData == null) return
      val baseDir = moduleData.baseDir ?: return

      runWriteAction {
        val directory = PsiManager.getInstance(project).findDirectory(baseDir)!!
        val templateName = when (sparkTypeProperty.get()) {
          SparkProjectType.BATCH -> "pyspark_batch"
          SparkProjectType.STREAMING -> "pyspark_stream"
        }
        val mainTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(templateName)
        val mainFile = FileTemplateUtil.createFromTemplate(mainTemplate, "main.py", null, directory) as PsiFile

        invokeLater {
          FileEditorManager.getInstance(project).apply {
            openFile(mainFile.virtualFile, true)
          }
        }
      }
    }

    private fun installRequirements(project: Project, sdk: Sdk) {
      PyPackageCoroutine.launch(project) {
        val manager = PythonPackageManager.forSdk(project, sdk)
        PythonPackageManagerUI.forPackageManager(manager).installPackagesBackground(requirements)
      }
    }
  }
}

class PySparkModuleBuilder : GeneratorNewProjectWizardBuilderAdapter(PySparkNewProjectWizard())
