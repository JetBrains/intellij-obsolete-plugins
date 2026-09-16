package com.jetbrains.spark.submit.run.local.ui

import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.enteredTextSatisfies
import com.jetbrains.bigdatatools.common.integration.python.BdtPythonIntegration
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SchemeFilePathSerializer
import com.jetbrains.spark.submit.model.SingleFilePathSerializer
import com.jetbrains.spark.submit.run.common.SparkArtifactUtil
import com.jetbrains.spark.submit.run.common.ui.SparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.run.local.LocalSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.local.LocalSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.JCheckBox
import javax.swing.JComponent

class LocalSparkSubmitConfigurationEditor(
  project: Project,
  val factory: LocalSparkSubmitConfigurationFactory
) : SparkSubmitConfigurationEditor<LocalSparkJobRunConfiguration>(project) {
  private val pyInterpreterComboBox = BdtPythonIntegration.getInstance()?.createPySdChoosingComponent(project)

  override val isInteractiveField: JCheckBox? = if (!SystemInfo.isWindows)
    createIsInteractiveField()
  else
    null

  override val sparkHomeField =
    LocalFileSelector(project,
                      FileSelectorType.LOCAL_DIRECTORY,
                      SingleFilePathSerializer(FileType.FILE),
                      SparkMessagesBundle.message("dialog.sparkHomePath.title"))

  override val workDirectoryField =
    LocalFileSelector(project,
                      FileSelectorType.LOCAL_DIRECTORY,
                      SingleFilePathSerializer(FileType.FILE),
                      SparkMessagesBundle.message("dialog.workDir.title"), getHomeDir())

  override val artifactPathField =
    LocalFileSelector(project,
                      FileSelectorType.LOCAL_JAR,
                      SchemeFilePathSerializer,
                      SparkMessagesBundle.message("dialog.artifactPath.title"), project.basePath)

  private val jarsField =
    LocalFileMultiSelector(project,
                           FileSelectorType.LOCAL_JAR,
                           SchemeFilePathSerializer,
                           SparkMessagesBundle.message("settings.dependencies.jars"),
                           SparkMessagesBundle.message("dialog.jars.title")).apply {
      toolTipText = SparkMessagesBundle.message("settings.dependencies.jars.hint")
    }

  private val filesField = LocalFileMultiSelector(project,
                                                  FileSelectorType.LOCAL_FILE,
                                                  SchemeFilePathSerializer,
                                                  SparkMessagesBundle.message("settings.dependencies.files"),
                                                  SparkMessagesBundle.message("dialog.files.title")).apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.files.hint")
  }

  private val pyFilesField = LocalFileMultiSelector(project,
                                                    FileSelectorType.LOCAL_FILE,
                                                    SchemeFilePathSerializer,
                                                    SparkMessagesBundle.message("settings.dependencies.python"),
                                                    SparkMessagesBundle.message("dialog.pyfiles.title")).apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.python.hint")
  }

  override val dependencyFields = listOf(jarsField, filesField, pyFilesField)

  override val keytabField =
    LocalFileSelector(project,
                      FileSelectorType.LOCAL_DIRECTORY,
                      SingleFilePathSerializer(FileType.FILE),
                      SparkMessagesBundle.message("dialog.keytabFile.title"),
                      getHomeDir())

  override val propertiesFileField =
    LocalFileSelector(project,
                      FileSelectorType.LOCAL_DIRECTORY,
                      SingleFilePathSerializer(FileType.FILE),
                      SparkMessagesBundle.message("dialog.propertiesFile.title"),
                      getHomeDir())

  override val driverLibraryPathField =
    LocalFileMultiSelector(project,
                           FileSelectorType.LOCAL_JAR,
                           SchemeFilePathSerializer,
                           SparkMessagesBundle.message("settings.driver.library.path"),
                           SparkMessagesBundle.message("dialog.driverLibraryPath.title"))

  override val driverClassPathField =
    LocalFileMultiSelector(project,
                           FileSelectorType.LOCAL_JAR,
                           SchemeFilePathSerializer,
                           SparkMessagesBundle.message("settings.driver.class.path"),
                           SparkMessagesBundle.message("dialog.driverClassPath.title"))

  private val archivesLocal =
    LocalFileMultiSelector(project,
                           FileSelectorType.LOCAL_FILE,
                           SchemeFilePathSerializer,
                           SparkMessagesBundle.message("settings.executor.archives"),
                           SparkMessagesBundle.message("dialog.archives.title"))

  override val archivesFields = listOf(archivesLocal)

  override fun createEditor(): JComponent {
    val panel = panel {
      finalCommandRow()

      row { cell(optionsLink).align(AlignX.RIGHT) }
      mainSparkSettingsRow()
      pyInterpreterComboBox?.let {
        row(SparkMessagesBundle.message("settings.spark.python.sdk"), pyInterpreterComboBox.getComponent())
          .visibleIf(artifactPathField.interactiveComponent.enteredTextSatisfies {
            it.endsWith(".py") || it.endsWith(".zip")
          })
      }
      optionBlocks()
      updateFieldsForManagerType()
    }
    return panel
  }

  override fun createTemporaryConfiguration() =
    factory.createTemplateConfiguration(project)

  override fun showShellExecutor() = !SystemInfo.isWindows

  override fun resolveArtifact(indicator: ProgressIndicator): String? {
    val path = artifactPathField.path
    return when (path.type) {
      FileType.FILE -> path.path
      FileType.ARTIFACT -> SparkArtifactUtil.getArtifactOut(project, path.path)
      else -> null
    }
  }

  override fun applyEditorTo(s: LocalSparkJobRunConfiguration) {
    s.jars = jarsField.files
    s.files = filesField.files
    s.pyFiles = pyFilesField.files

    s.archives = archivesLocal.files

    val selectedPythonPath = pyInterpreterComboBox?.selectedSdkPath
    selectedPythonPath?.let {
      s.pythonSdkPath = it
    }
    s.sparkMonitoringDriverId = sparkMonitoringId()

    super.applyEditorTo(s)
  }

  override fun resetEditorFrom(s: LocalSparkJobRunConfiguration) {
    if (s.sparkHome.isNotBlank()) {
      sparkHomeField.path = FilePath(FileType.FILE, s.sparkHome)
    }
    else {
      sparkHomeField.component.text = ""
    }

    jarsField.files = s.jars
    filesField.files = s.files
    pyFilesField.files = s.pyFiles

    archivesLocal.files = s.archives

    pyInterpreterComboBox?.selectedSdkPath = s.pythonSdkPath

    initSparkMonitoringComboBox(s.sparkMonitoringDriverId)
    super.resetEditorFrom(s)
  }
}

@NlsSafe
private fun getHomeDir() = VfsUtil.getUserHomeDir()?.path