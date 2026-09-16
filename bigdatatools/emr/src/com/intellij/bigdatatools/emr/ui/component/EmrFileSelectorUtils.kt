package com.intellij.bigdatatools.emr.ui.component

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.bigdatatools.common.rfs.copypaste.RfsCopyPasteManager
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.ui.RfsChooserDescriptor
import com.jetbrains.bigdatatools.common.rfs.ui.RfsDirOnlyDescriptor
import com.jetbrains.bigdatatools.common.rfs.ui.RfsFileChooser
import com.jetbrains.bigdatatools.common.rfs.util.withPrefixSlash
import com.jetbrains.bigdatatools.sftp.rfs.driver.SftpDriver
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorOptionBase
import com.jetbrains.spark.submit.model.FileSelectorReturningOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.DefaultSelectedArtifactInfo
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.com.jetbrains.bigdatatools.aws.s3.S3Driver
import java.util.function.Consumer

internal object EmrFileSelectorUtils {
  val EMR_UPLOAD_WITH_S3 = FileSelectorType("file.selector.dataproc-upload.withS3")
  val EMR_UPLOAD_WITHOUT_S3 = FileSelectorType("file.selector.dataproc-upload.withoutS3")
}

internal class FileSelectorOptionS3 : FileSelectorReturningOption(FileType.S3,
                                                                  SparkMessagesBundle.message("settings.url.s3.name"),
                                                                  SparkMessagesBundle.message("settings.url.s3.name"),
                                                                  RfsIcons.S3_ICON) {
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    require(context is EmrFileSelectorContext)
    val s3Driver = runWithModalProgressBlocking(context.project, MessagesBundle.message("depend.connection.progress")) { context.driversProvider.getOrCreateS3Driver() }
    val chooser = RfsFileChooser(mainTitle = MessagesBundle.message("file.chooser.source.file.selector.title"),
                                 project = context.driversProvider.project,
                                 descriptor = RfsChooserDescriptor(),
                                 preselectedDriver = s3Driver,
                                 preselectedPath = context.prevSelectedPath ?: "",
                                 drivers = listOf(s3Driver) + getExistsS3Drivers(context.driversProvider.project))

    val resultPath = chooser.showAndGetResult()?.firstOrNull() ?: return null
    return DefaultSelectedArtifactInfo(FilePath(FileType.S3, resultPath.path.stringRepresentation()))
  }
  override val applicableForTypes: List<FileSelectorType> get() = listOf(EmrFileSelectorUtils.EMR_UPLOAD_WITH_S3)
}

class FileSelectorOptionServerEmr : FileSelectorReturningOption(FileType.SERVER,
                                                                SparkMessagesBundle.message("settings.url.server.name"),
                                                                SparkMessagesBundle.message("settings.url.server.tooltip"),
                                                                AllIcons.Nodes.ResourceBundle) {
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    require(context is EmrFileSelectorContext)
    val sftpDriver = runWithModalProgressBlocking(context.project, MessagesBundle.message("depend.connection.progress")) { context.driversProvider.getOrCreateSftpDriver() }

    val targetDrivers = listOfNotNull(sftpDriver) + getExistsSftpDrivers(context.driversProvider.project)
    targetDrivers.ifEmpty { return null }

    val chooser = RfsFileChooser(mainTitle = MessagesBundle.message("file.chooser.source.file.selector.title"),
                                 project = context.project,
                                 descriptor = RfsChooserDescriptor(),
                                 preselectedDriver = sftpDriver,
                                 preselectedPath = context.prevSelectedPath ?: "",
                                 drivers = targetDrivers)
    val fileInfo = chooser.showAndGetResult()?.firstOrNull() ?: return null

    val resultPath = fileInfo.path.stringRepresentation().withPrefixSlash()
    return DefaultSelectedArtifactInfo(FilePath(FileType.SERVER, resultPath))
  }
  override val applicableForTypes: List<FileSelectorType> get() = listOf(EmrFileSelectorUtils.EMR_UPLOAD_WITH_S3, EmrFileSelectorUtils.EMR_UPLOAD_WITHOUT_S3)
}

abstract class FileSelectorOptionUploadEmr(private val includeS3: Boolean) : FileSelectorOptionBase(FileType.UPLOAD,
                                                                                                    SparkMessagesBundle.message(
                                                                                                      "settings.url.upload.name"),
                                                                                                    SparkMessagesBundle.message(
                                                                                                      "settings.url.upload.tooltip"),
                                                                                                    AllIcons.Actions.Upload) {
  override fun select(context: FileSelectorContext, consumer: Consumer<SelectedArtifactInfo>) {
    require(context is EmrFileSelectorContext)
    val sftpDriver = runWithModalProgressBlocking(context.project, MessagesBundle.message("depend.connection.progress")) { context.driversProvider.getOrCreateSftpDriver() }
    val s3Driver = if (includeS3) runWithModalProgressBlocking(context.project, MessagesBundle.message("depend.connection.progress")) { context.driversProvider.getOrCreateS3Driver() } else null
    val targetDrivers = listOfNotNull(sftpDriver, s3Driver) +
                        getExistsS3Drivers(context.project) +
                        getExistsSftpDrivers(context.project)

    if (targetDrivers.isEmpty())
      return

    val uploadFile = FileSelector.openLocalFile(MessagesBundle.message("file.chooser.source.file.selector.title"), context.project,
                                                context.prevSelectedPath) ?: return

    val chooser = RfsFileChooser(
      project = context.project,
      descriptor = RfsDirOnlyDescriptor(false),
      mainTitle = context.dialogTitle,
      drivers = targetDrivers,
      preselectedDriver = targetDrivers.first(),
      preselectedPath = ""
    )
    val targetFileInfo = chooser.showAndGetResult()?.firstOrNull() ?: return
    val targetDriver = targetFileInfo.driver
    val targetPath = targetFileInfo.path

    RfsCopyPasteManager.uploadFromDisk(context.project,
                                       listOf(uploadFile),
                                       targetDriver = targetDriver,
                                       targetPath = targetPath,
                                       runInBackground = false) {
      val uploadedPath = it.firstOrNull() ?: return@uploadFromDisk
      val res = when (targetDriver) {
        is SftpDriver -> FilePath(FileType.SERVER, uploadedPath.stringRepresentation())
        is S3Driver -> FilePath(FileType.S3, uploadedPath.stringRepresentation())
        else -> return@uploadFromDisk
      }
      withContext(Dispatchers.EDT) {
        consumer.accept(DefaultSelectedArtifactInfo(res))
      }
    }
    return
  }
}

class FileSelectorOptionUploadEmrWithS3 : FileSelectorOptionUploadEmr(true) {
  override val applicableForTypes get() = listOf(EmrFileSelectorUtils.EMR_UPLOAD_WITH_S3)
}
class FileSelectorOptionUploadEmrWithoutS3 : FileSelectorOptionUploadEmr(false) {
  override val applicableForTypes get() = listOf(EmrFileSelectorUtils.EMR_UPLOAD_WITHOUT_S3)
}

private fun getExistsSftpDrivers(project: Project) =
  DriverManager.getDrivers(project).filterIsInstance<SftpDriver>()

private fun getExistsS3Drivers(project: Project) =
  DriverManager.getDrivers(project).filterIsInstance<S3Driver>()