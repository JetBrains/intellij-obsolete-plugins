package com.jetbrains.spark.submit.run.ssh.upload

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findDocument
import com.jetbrains.bigdatatools.common.rfs.driver.ProgressOptions
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.bigdatatools.common.rfs.driver.blockingGet
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.toKotlinResult
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.common.SparkArtifactUtil
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.util.SshUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadBeforeRunTaskProvider : BeforeRunTaskProvider<UploadBeforeRunTask>() {
  override fun getId(): Key<UploadBeforeRunTask> = ID

  override fun getName(): String = SparkMessagesBundle.message("upload.files.through.sftp.to.spark.host")

  override fun createTask(runConfiguration: RunConfiguration) = if (runConfiguration is SshAwareSparkJobRunConfiguration)
    UploadBeforeRunTask()
  else
    null

  private fun getNotificationGroup() = NotificationGroupManager.getInstance().getNotificationGroup("SftpSparkFileUpload")

  override fun executeTask(context: DataContext,
                           configuration: RunConfiguration,
                           environment: ExecutionEnvironment,
                           task: UploadBeforeRunTask): Boolean {
    if (configuration !is SshAwareSparkJobRunConfiguration) return false

    val project = configuration.project
    val pathsForUpload = ApplicationManager.getApplication().runReadAction(Computable {
      configuration.getAllPaths()
        .mapNotNull {
          when (it.type) {
            FileType.ARTIFACT -> SparkArtifactUtil.getArtifactOut(project, it.path)
            FileType.UPLOAD -> it.path
            else -> null
          }
        }
    })
    if (pathsForUpload.isEmpty())
      return true

    return SafeExecutor.instance.asyncSuspendProgress(ProgressOptions(SparkMessagesBundle.message("upload.file.title"), project, true)) {
      val documents = readAction {
        pathsForUpload.mapNotNull { path ->
          path.toNioPathOrNull()?.let { VfsUtil.findFile(it, true) }?.findDocument()
        }
      }
      withContext(Dispatchers.EDT) {
        documents.forEach { document ->
          FileDocumentManager.getInstance().saveDocument(document)
        }
      }

      val sshConfig = configuration.getSshConfig() ?: error(SparkMessagesBundle.message("setup.ssh.config"))

      val targetDirectory = SshUtils.prepareTargetDir(configuration.targetDirectory.path, sshConfig)
      SshUtils.uploadFiles(project, sshConfig, pathsForUpload, targetDirectory)
    }.blockingGet().toKotlinResult().onFailure {
      if (it !is CancellationException) {
        getNotificationGroup().createNotification(
          SparkMessagesBundle.message("upload.files.error", it.message ?: it::class.java.toString()),
          NotificationType.ERROR).notify(project)
      }
    }.isSuccess
  }

  companion object {
    val ID: Key<UploadBeforeRunTask> = Key.create("SftpSparkFileUpload")
  }
}