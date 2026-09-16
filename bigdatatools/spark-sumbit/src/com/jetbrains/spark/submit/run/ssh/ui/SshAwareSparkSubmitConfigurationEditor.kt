package com.jetbrains.spark.submit.run.ssh.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SchemeFilePathSerializer
import com.jetbrains.spark.submit.model.SingleFilePathSerializer
import com.jetbrains.spark.submit.run.common.SparkArtifactUtil
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import com.jetbrains.spark.submit.run.common.ui.SparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.util.SshUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle

abstract class SshAwareSparkSubmitConfigurationEditor<SparkJobRunConfType : SshAwareSparkJobRunConfiguration>(
  project: Project
) : SparkSubmitConfigurationEditor<SparkJobRunConfType>(project), SshConfigProvider {
  protected val myDisposable: Disposable get() = this

  abstract override suspend fun getSshConfig(): SshConfig?

  /** Remote folder, where all dependencies, artifacts and other files will be copied before execution.
   * Each file can override this value. */
  val targetDirectory: FileSelector = SshFileSelector(SparkMessagesBundle.message("dialog.targetDirectory.title"),
                                                         project,
                                                         FileSelectorType.SSH_DIRECTORY,
                                                         SingleFilePathSerializer(FileType.SERVER),
                                                         defaultText = "\$HOME") {
    getSshConfig()
  }.apply {
    toolTipText = SparkMessagesBundle.message("settings.ssh.target.dir.hint")
  }

  public final override val sparkHomeField: FileSelector = SshFileSelector(SparkMessagesBundle.message("dialog.sparkHomePath.title"),
                                                                              project,
                                                                              FileSelectorType.SSH_DIRECTORY,
                                                                              SingleFilePathSerializer(FileType.SERVER)) {
    getSshConfig()
  }

  public final override val workDirectoryField: FileSelector = SshFileSelector(SparkMessagesBundle.message("dialog.workDir.title"),
                                                                                  project,
                                                                                  FileSelectorType.SSH_DIRECTORY,
                                                                                  SingleFilePathSerializer(FileType.SERVER)) {
    getSshConfig()
  }

  val jarsField = SshFileMultiSelector(project,
                                                             FileSelectorType.SSH_JAR,
                                                             SchemeFilePathSerializer,
                                                             SparkMessagesBundle.message("settings.dependencies.jars"),
                                                             SparkMessagesBundle.message("dialog.jars.title")) {
    getSshConfig()
  }.apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.jars.hint")
  }

  val filesField = SshFileMultiSelector(project,
                                                              FileSelectorType.SSH_FILE,
                                                              SchemeFilePathSerializer,
                                                              SparkMessagesBundle.message("settings.dependencies.files"),
                                                              SparkMessagesBundle.message("dialog.files.title")) {
    getSshConfig()
  }.apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.files.hint")
  }

  val pyFilesField = SshFileMultiSelector(project,
                                                                FileSelectorType.SSH_FILE,
                                                                SchemeFilePathSerializer,
                                                                SparkMessagesBundle.message("settings.dependencies.python"),
                                                                SparkMessagesBundle.message("dialog.pyfiles.title")) {
    getSshConfig()
  }.apply {
    toolTipText = SparkMessagesBundle.message("settings.dependencies.python.hint")
  }

  final override val dependencyFields = listOf(jarsField, filesField, pyFilesField)

  public final override val keytabField: FileSelector =
    SshFileSelector(SparkMessagesBundle.message("dialog.keytabFile.title"),
                    project, FileSelectorType.SSH_FILE, SchemeFilePathSerializer) {
      getSshConfig()
    }

  public final override val propertiesFileField: FileSelector =
    SshFileSelector(SparkMessagesBundle.message("dialog.propertiesFile.title"),
                    project, FileSelectorType.SSH_FILE, SchemeFilePathSerializer) {
      getSshConfig()
    }

  public final override val driverLibraryPathField = SshFileMultiSelector(project,
                                                                                                FileSelectorType.SSH_FILE,
                                                                                                SchemeFilePathSerializer,
                                                                                                SparkMessagesBundle.message(
                                                                                           "settings.driver.library.path"),
                                                                                                SparkMessagesBundle.message(
                                                                                           "dialog.driverLibraryPath.title")) {
    getSshConfig()
  }

  public final override val driverClassPathField = SshFileMultiSelector(project,
                                                                                              FileSelectorType.SSH_FILE,
                                                                                              SchemeFilePathSerializer,
                                                                                              SparkMessagesBundle.message(
                                                                                         "settings.driver.class.path"),
                                                                                              SparkMessagesBundle.message(
                                                                                         "dialog.driverClassPath.title")) {
    getSshConfig()
  }

  internal val archivesField = SshFileMultiSelector(project,
                                                                          FileSelectorType.SSH_FILE,
                                                                          SchemeFilePathSerializer,
                                                                          SparkMessagesBundle.message("settings.executor.archives"),
                                                                          SparkMessagesBundle.message("dialog.archives.title")) {
    getSshConfig()
  }

  final override val archivesFields = listOf(archivesField)

  override fun resolveArtifact(indicator: ProgressIndicator): String? {
    indicator.text = artifactPathField.path.toString()

    val artifactPath = artifactPathField.path
    return when (artifactPath.type) {
      FileType.SERVER -> {
        val sshConfig = runBlockingCancellable { getSshConfig() } ?: return null
        SshUtils.downloadFileToTempDirSync(project, sshConfig, artifactPath.path).canonicalPath
      }
      FileType.UPLOAD -> artifactPath.path
      FileType.ARTIFACT -> SparkArtifactUtil.getArtifactOut(project, artifactPath.path)
      else -> null
    }
  }

  override fun resetEditorFrom(s: SparkJobRunConfType) {
    targetDirectory.path = s.targetDirectory

    sparkHomeField.path = FilePath(FileType.SERVER, s.sparkHome)

    jarsField.files = s.jars
    filesField.files = s.files
    pyFilesField.files = s.pyFiles
    archivesField.files = s.archives

    super.resetEditorFrom(s)
  }

  override fun applyEditorTo(s: SparkJobRunConfType) {
    s.targetDirectory = targetDirectory.path

    s.jars = jarsField.files
    s.files = filesField.files
    s.pyFiles = pyFilesField.files

    s.archives = archivesField.files

    super.applyEditorTo(s)
  }
}