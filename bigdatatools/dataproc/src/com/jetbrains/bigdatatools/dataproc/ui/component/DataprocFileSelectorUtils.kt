package com.jetbrains.bigdatatools.dataproc.ui.component

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.copypaste.RfsCopyPasteManager
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.ui.RfsChooserDescriptor
import com.jetbrains.bigdatatools.common.rfs.ui.RfsDirOnlyDescriptor
import com.jetbrains.bigdatatools.common.rfs.ui.RfsFileChooser
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.sftp.rfs.driver.SftpDriver
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorOptionBase
import com.jetbrains.spark.submit.model.FileSelectorReturningOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.DefaultSelectedArtifactInfo
import com.jetbrains.spark.submit.run.common.ui.FileMultiSelector
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.com.jetbrains.bigdatatools.gcs.GcsDriver
import java.util.function.Consumer

object DataprocFileSelectorUtils {
  fun FileMultiSelector.withDataprocFsValidator(uiDisposable: Disposable): FileMultiSelector {
    withValidator(uiDisposable) {
      files.firstNotNullOfOrNull {
        validatePathFs(it)
      }
    }
    return this
  }

  fun FileMultiSelector.withDataprocArchiveValidator(uiDisposable: Disposable): FileMultiSelector {
    withValidator(uiDisposable) {
      files.firstNotNullOfOrNull {
        validatePathIsArchive(it)
      }
    }
    return this
  }

  val DATAPROC_UPLOAD = FileSelectorType("file.selector.dataproc-upload")

  private fun validatePathFs(path: FilePath) = if (path.toString().split("://").first().lowercase() !in setOf("gs", "hdfs", "file"))
    DataprocMessagesBundle.message("job.validation.file.fs", path)
  else
    null

  private fun validatePathIsArchive(filePath: FilePath): String? {
    val path = filePath.path
    return if (path.endsWith(".jar") ||
               path.endsWith(".tar") ||
               path.endsWith(".tar.gz") ||
               path.endsWith(".tgz") ||
               path.endsWith(".zip"))
      null
    else DataprocMessagesBundle.message("job.validation.file.archive", path)
  }

}

class FileSelectorOptionUploadDataproc : FileSelectorOptionBase(FileType.UPLOAD,
                                                                SparkMessagesBundle.message("settings.url.upload.name"),
                                                                SparkMessagesBundle.message("settings.url.upload.tooltip"),
                                                                AllIcons.Actions.Upload) {
  override fun select(context: FileSelectorContext, consumer: Consumer<SelectedArtifactInfo>) {
    require(context is DataprocFileSelectorContext)
    val uploadFile = FileSelector.openLocalFile(MessagesBundle.message("file.chooser.source.file.selector.title"), context.project, context.prevSelectedPath) ?: return

    val drivers = listOf(context.driversProvider.gcDriver) + getExistsGcsDrivers(context.driversProvider.project)

    val chooser = RfsFileChooser(
      project = context.project,
      descriptor = RfsDirOnlyDescriptor(false),
      mainTitle = context.dialogTitle,
      drivers = drivers,
      preselectedDriver = drivers.first(),
      preselectedPath = ""
    )
    val targetFileInfo = chooser.showAndGetResult()?.firstOrNull() ?: return
    val targetDriver = targetFileInfo.driver
    val targetPath = targetFileInfo.path

    RfsCopyPasteManager.uploadFromDisk(context.driversProvider.project,
                                       listOf(uploadFile),
                                       targetDriver = targetDriver,
                                       targetPath = targetPath,
                                       runInBackground = false) {
      val uploadedPath = it.firstOrNull() ?: return@uploadFromDisk
      val res = when (targetDriver) {
        is SftpDriver -> FilePath(FileType.SERVER, uploadedPath.stringRepresentation())
        is GcsDriver -> FilePath(FileType.GCS, uploadedPath.stringRepresentation())
        else -> return@uploadFromDisk
      }
      withContext(Dispatchers.EDT) {
        consumer.accept(DefaultSelectedArtifactInfo(res))
      }
    }
  }
  override val applicableForTypes get() = listOf(DataprocFileSelectorUtils.DATAPROC_UPLOAD)
}

class FileSelectorOptionGcs : FileSelectorReturningOption(FileType.GCS,
                                                          SparkMessagesBundle.message("settings.url.gcs.name"),
                                                          SparkMessagesBundle.message("settings.url.gcs.tooltip"),
                                                          RfsIcons.GCS_ICON) {
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    require(context is DataprocFileSelectorContext)
    return selectGcsFile(context.prevSelectedPath, context.driversProvider)?.let { DefaultSelectedArtifactInfo(it) }
  }
  override val applicableForTypes get() = listOf(DataprocFileSelectorUtils.DATAPROC_UPLOAD)
}

private fun getExistsGcsDrivers(project: Project) =
  DriverManager.getDrivers(project).filterIsInstance<GcsDriver>()

private fun selectGcsFile(prevSelectedPath: String?, driverProviders: DataprocDriversProvider): FilePath? {
  val chooser = RfsFileChooser(mainTitle = MessagesBundle.message("file.chooser.source.file.selector.title"),
                               project = driverProviders.project,
                               descriptor = RfsChooserDescriptor(),
                               preselectedDriver = driverProviders.gcDriver,
                               preselectedPath = prevSelectedPath ?: "",
                               drivers = listOf(driverProviders.gcDriver) + getExistsGcsDrivers(driverProviders.project))

  val resultPath = chooser.showAndGetResult()?.firstOrNull() ?: return null
  return FilePath(FileType.GCS, resultPath.path.stringRepresentation())
}