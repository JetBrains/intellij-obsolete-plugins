package com.intellij.bigdatatools.emr.submit

import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.dependend.drivers.EmrDriversProviderImpl
import com.intellij.bigdatatools.emr.ui.component.EmrComponentsCreator
import com.intellij.bigdatatools.emr.ui.component.EmrFileMultiSelector
import com.intellij.bigdatatools.emr.ui.component.EmrFileSelector
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.TimerUtil
import com.jetbrains.bigdatatools.common.rfs.copypaste.RfsCopyPasteManager
import com.intellij.bigdatatools.coreUi.ui.row
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import com.jetbrains.spark.submit.run.common.ui.SparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.run.common.ui.row
import com.jetbrains.spark.submit.run.common.ui.withNonEmptyValidator
import com.jetbrains.spark.submit.settings.RunConfigurationBlockType
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import software.amazon.awssdk.services.emr.model.ActionOnFailure
import javax.swing.JComponent

class EmrSparkSubmitConfigurationEditor(project: Project,
                                        dataManager: EmrDataManager,
                                        clusterId: String?,
                                        val configuration: EmrSparkJobRunConfiguration) :
  SparkSubmitConfigurationEditor<EmrSparkJobRunConfiguration>(project) {

  private val driversProvider = EmrDriversProviderImpl(project, dataManager, clusterId).also {
    Disposer.register(this, it)
  }

  override val sparkHomeField: FileSelector? = null
  override val workDirectoryField: FileSelector? = null

  private val nameField = JBTextField(SHORT_TEXT_FIELD_COLUMNS)

  private val actionOnFailureField = EmrComponentsCreator.createActionOnFailureField()

  override val artifactPathField = EmrFileSelector(
    SparkMessagesBundle.message("dialog.artifactPath.title"),
    project, driversProvider, withS3 = true
  ).withNonEmptyValidator(this)

  private val jarsField = EmrFileMultiSelector(
    project = project,
    label = SparkMessagesBundle.message("settings.dependencies.jars"),
    title = SparkMessagesBundle.message("dialog.jars.title"),
    driversProvider = driversProvider).apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.jars.hint")
  }

  private val filesField = EmrFileMultiSelector(
    project = project,
    label = SparkMessagesBundle.message("settings.dependencies.files"),
    title = SparkMessagesBundle.message("dialog.files.title"),
    driversProvider = driversProvider).apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.files.hint")
  }

  private val pyFilesField = EmrFileMultiSelector(
    project = project,
    label = SparkMessagesBundle.message("settings.dependencies.python"),
    title = SparkMessagesBundle.message("dialog.pyfiles.title"),
    driversProvider = driversProvider).apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.python.hint")
  }

  override val dependencyFields = listOf(jarsField, filesField, pyFilesField)

  override val keytabField = EmrFileSelector(SparkMessagesBundle.message("dialog.keytabFile.title"),
                                             project,
                                             driversProvider,
                                             withS3 = false)

  override val propertiesFileField = EmrFileSelector(SparkMessagesBundle.message("dialog.propertiesFile.title"),
                                                     project,
                                                     driversProvider,
                                                     withS3 = false)

  override val driverLibraryPathField = EmrFileMultiSelector(
    project = project,
    label = SparkMessagesBundle.message("settings.driver.library.path"),
    title = SparkMessagesBundle.message("dialog.driverLibraryPath.title"),
    driversProvider = driversProvider)

  override val driverClassPathField = EmrFileMultiSelector(
    project = project,
    label = SparkMessagesBundle.message("settings.driver.class.path"),
    title = SparkMessagesBundle.message("dialog.driverClassPath.title"),
    driversProvider = driversProvider)

  private val archivesField = EmrFileMultiSelector(
    project = project,
    label = SparkMessagesBundle.message("settings.executor.archives"),
    title = SparkMessagesBundle.message("dialog.archives.title"),
    driversProvider = driversProvider)

  override val archivesFields = listOf(archivesField)

  private val updater = TimerUtil.createNamedTimer("EMR result string update", 1000).apply {
    isRepeats = true
    addActionListener {
      val tempConf = EmrSparkJobRunConfiguration(configuration.project, configuration.factory!!, configuration.name)
      applyEditorTo(tempConf)
    }
  }

  init {
    updater.start()

    Disposer.register(this) {
      updater.stop()
    }
  }

  override fun Panel.mainSparkSettingsRow() {
    row(HdfsMessagesBundle.message("emr.spark.submit.editor.name"), nameField)

    artifactRows(needGap = true)
    deployMainRows()
  }

  override fun createTemporaryConfiguration() =
    EmrSparkSubmitConfigurationFactory.createTemplateConfiguration(project)

  override fun createEditor(): JComponent {
    val panel = panel {
      row { cell(optionsLink).align(AlignX.RIGHT) }
      mainSparkSettingsRow()

      optionBlock(RunConfigurationBlockType.SPARK_CONFIG) { sparkConfSettingsRow() }
      optionBlock(RunConfigurationBlockType.DEPENDENCIES) { dependencySettingsRow() }
      optionBlock(RunConfigurationBlockType.MAVEN_DEPENDENCIES) { mavenSettingsRow() }
      optionBlock(RunConfigurationBlockType.DRIVER) { driverSettingsRow() }
      optionBlock(RunConfigurationBlockType.EXECUTOR) { executorSettingsRow() }
      optionBlock(RunConfigurationBlockType.KERBEROS) { kerberosSettingsRow() }
      optionBlock(RunConfigurationBlockType.ADDITIONAL) { additionalSparkSettingsRow() }

      finalCommandRow()

      resetEditorFrom(configuration)
    }

    return panel
  }

  override fun resetEditorFrom(s: EmrSparkJobRunConfiguration) {
    nameField.text = s.name

    jarsField.files = s.jars
    filesField.files = s.files
    pyFilesField.files = s.pyFiles
    archivesField.files = s.archives

    super.resetEditorFrom(s)
  }

  override fun applyEditorTo(s: EmrSparkJobRunConfiguration) {
    s.name = nameField.text
    s.actionOnFailure = actionOnFailureField.selectedItem as ActionOnFailure

    s.jars = jarsField.files
    s.files = filesField.files
    s.pyFiles = pyFilesField.files

    s.archives = archivesField.files

    super.applyEditorTo(s)
  }

  @Suppress("DuplicatedCode")
  override fun Panel.additionalSparkSettingsRow() {
    optionBlock(RunConfigurationBlockType.ADDITIONAL) {
      clusterManagerField()

      proxyUserField.toolTipText = SparkMessagesBundle.message("settings.cluster.manager.proxy.user.hint")
      row(SparkMessagesBundle.message("settings.cluster.manager.proxy.user"), proxyUserField)

      driverJavaOptionsField.textField.toolTipText = SparkMessagesBundle.message("settings.driver.java.options.hint")
      row(SparkMessagesBundle.message("settings.driver.java.options"), driverJavaOptionsField)

      driverLibraryPathField.toolTipText = SparkMessagesBundle.message("settings.driver.library.path.hint")
      row(driverLibraryPathField)

      driverClassPathField.toolTipText = SparkMessagesBundle.message("settings.driver.class.path.hint")
      row(driverClassPathField)

      archivesRow = rowsRange {
        archivesFields.forEach {
          it.toolTipText = SparkMessagesBundle.message("settings.executor.archives.hint")
          row(it)
        }
      }

      superviseRow = rowsRange {
        row {
          cell(superviseField)
          comment(SparkMessagesBundle.message("settings.cluster.manager.supervise.hint"))
        }
      }

      queueRow = rowsRange {
        queueField.toolTipText = SparkMessagesBundle.message("settings.cluster.manager.queue.hint")
        queueField.emptyText.text = "default"
        row(SparkMessagesBundle.message("settings.cluster.manager.queue"), queueField)
      }

      rowsRange {
        block(verboseField)
      }
    }
  }

  override fun resolveArtifact(indicator: ProgressIndicator): String? {
    val artifactPath = artifactPathField.path

    return when (artifactPath.type) {
      FileType.S3 -> {
        val file = FileUtil.createTempFile("aws-submit", "temp-jar.jar", true)
        val driver = runBlockingCancellable { driversProvider.getOrCreateS3Driver() }
        val sourceRfsPath = driver.createRfsPath(artifactPath.path)
        RfsCopyPasteManager.downloadFromRemoteToIoFile(project, indicator, driver, sourceRfsPath, file)
      }
      FileType.CUSTOM -> {
        val file = FileUtil.createTempFile("aws-submit", "temp-jar.jar", true)
        val driver = runBlockingCancellable { driversProvider.getOrCreateSftpDriver() } ?: return null
        val sourceRfsPath = driver.createRfsPath(artifactPath.path)
        RfsCopyPasteManager.downloadFromRemoteToIoFile(project, indicator, driver, sourceRfsPath, file)
      }
      FileType.UPLOAD -> artifactPath.path
      else -> null
    }
  }
}

