package com.jetbrains.spark.submit.model

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.spark.submit.run.cluster.DefaultSelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.UploadExistingSelectedArtifactInfo
import com.jetbrains.spark.submit.run.common.ui.FileSelector
import com.jetbrains.spark.submit.run.local.ui.defaultLocalPath
import com.jetbrains.spark.submit.run.ssh.ui.SshFileSelectorContext
import com.jetbrains.spark.submit.run.ssh.util.SshSparkEditorUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import com.jetbrains.spark.submit.util.SparkSubmitSupportUtils
import org.jetbrains.annotations.Nls
import javax.swing.Icon

data class FileType(val schemeNoSlash: String) {
  val scheme: String get() = if (schemeNoSlash.isEmpty()) schemeNoSlash else "$schemeNoSlash://"

  companion object {

    fun migrateFromEnum(enumMemberName: String): FileType {
      return when (enumMemberName) {
        "CUSTOM" -> CUSTOM
        "GCS" -> GCS
        else -> FileType(enumMemberName.lowercase())
      }
    }

    //file which will be upload to server by SSH
    val UPLOAD = FileType("upload")

    //file which must exists on server
    val SERVER = FileType("server")

    //file which will be build from IDEA artifact
    val ARTIFACT = FileType("artifact")

    //a URI starting with file: is expected to exist as a local file on each worker node.
    // it is a strange for me but right path is file:/opt/super.jar but not file://opt/super.jar
    val FILE = FileType("file")
    val HDFS = FileType("hdfs")
    val HTTP = FileType("http")
    val HTTPS = FileType("https")
    val FTP = FileType("ftp")
    val S3 = FileType("s3")
    val GCS = FileType("gs")
    val CUSTOM = FileType("")

    val WEB_TYPES = listOf(HTTP, HTTPS, FTP, HDFS).map { it }

    val ALL = listOf(UPLOAD, SERVER, FILE).map { it }
  }

}

class FileSelectorOptionSshServer : FileSelectorReturningOption(FileType.SERVER,
                                                                SparkMessagesBundle.message("settings.url.server.name"),
                                                                SparkMessagesBundle.message("settings.url.server.tooltip"),
                                                                AllIcons.Nodes.ResourceBundle) {

  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    require(context is SshFileSelectorContext)
    val sshConfig = runWithModalProgressBlocking(context.project, MessagesBundle.message("ssh.state.init")) {
      context.revalidateFormAndGetSshConfig()
    } ?: return null
    return showSshDialog(context.dialogTitle, context.project, sshConfig, context.prevSelectedPath)?.let {
      DefaultSelectedArtifactInfo(FilePath(FileType.SERVER, it))
    }
  }
  private fun showSshDialog(@Nls(capitalization = Nls.Capitalization.Title) title: String,
                            project: Project,
                            sshConfig: SshConfig,
                            prevSelectedPath: String? = null) =
    if (SparkSubmitSupportUtils.isWebDeploymentSupported()) try {
      SshSparkEditorUtils.selectSingleHostFile(project, sshConfig, title, prevSelectedPath)
    }
    catch (t: NoClassDefFoundError) {
      openRemoteFileMock(project, title, prevSelectedPath)
    }
    else {
      openRemoteFileMock(project, title, prevSelectedPath)
    }

  private fun openRemoteFileMock(project: Project,
                                 @Nls(capitalization = Nls.Capitalization.Title) dialogTitle: String,
                                 prevSelectedPath: String? = null): String? {
    // val title = SparkMessagesBundle.message("settings.url.server.mock.title")
    return Messages.showInputDialog(project, SparkMessagesBundle.message("settings.url.server.mock.desc"), dialogTitle,
                                    Messages.getQuestionIcon(), prevSelectedPath, NonEmptyInputValidator())
  }
  override val applicableForTypes get() = listOf(FileSelectorType.SSH_JAR, FileSelectorType.SSH_FILE, FileSelectorType.SSH_DIRECTORY, FileSelectorType.CLUSTER_JAR)
}

abstract class FileSelectorOptionUploadBase(
  icon: Icon
) : FileSelectorReturningOption(FileType.UPLOAD, SparkMessagesBundle.message("settings.url.upload.name"), SparkMessagesBundle.message("settings.url.upload.tooltip"), icon) {
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    return FileSelector.openLocalFile(context.dialogTitle, context.project, context.prevSelectedPath)?.let {
      UploadExistingSelectedArtifactInfo(it.path)
    }
  }
}

class FileSelectorOptionUpload : FileSelectorOptionUploadBase(AllIcons.Actions.Upload) {
  override val applicableForTypes get() = listOf(FileSelectorType.SSH_JAR, FileSelectorType.SSH_FILE)
}

class FileSelectorOptionUploadCluster : FileSelectorOptionUploadBase(AllIcons.General.OpenDisk) {
  override val applicableForTypes get() = listOf(FileSelectorType.CLUSTER_JAR)
}

class FileSelectorOptionLocalFile : FileSelectorReturningOption(FileType.FILE,
                                                                SparkMessagesBundle.message("settings.url.file.name"),
                                                                SparkMessagesBundle.message("settings.url.file.name"),
                                                                AllIcons.FileTypes.Any_type) {
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    val initialPath = context.prevSelectedPath ?: context.defaultLocalPath
    return FileSelector.openLocalFile(context.dialogTitle, context.project, initialPath)?.let {
      DefaultSelectedArtifactInfo(FilePath(FileType.FILE, it.path))
    }
  }
  override val applicableForTypes get() = listOf(FileSelectorType.LOCAL_JAR, FileSelectorType.LOCAL_FILE, FileSelectorType.LOCAL_DIRECTORY)
}