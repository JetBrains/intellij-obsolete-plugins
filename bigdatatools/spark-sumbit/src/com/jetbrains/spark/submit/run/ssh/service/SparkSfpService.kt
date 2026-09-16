package com.jetbrains.spark.submit.run.ssh.service

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.RawProgressReporter
import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.platform.util.progress.withProgressText
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.SftpProgressTracker
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.connectionBuilder
import com.intellij.ssh.ui.unified.SshUiData
import com.jetbrains.bigdatatools.common.util.SizeUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Service to transfer files from local to host and vice versa
 */
class SparkSfpService(val project: Project, private val sshConfig: SshConfig) : Disposable {
  private val sftpChannel = createSftpChannel()

  override fun dispose() = sftpChannel.close()

  fun getInfo(path: String): RemoteFileObject = sftpChannel.file(path)

  suspend fun uploadFile(path: String,
                         defaultTargetDirectory: String) {
    assert(sftpChannel.isConnected)
    val uploadFile = File(path)
    withProgressText(SparkMessagesBundle.message("progress.text.upload.to.host", uploadFile.name)) {
      reportRawProgress { reporter ->
        runInterruptible(Dispatchers.IO) {
          val progressTracker = SparSftpUploadProgressAdapter(uploadFile, reporter, coroutineContext.job)
          sftpChannel.uploadFileOrDir(uploadFile, defaultTargetDirectory, "", progressTracker, null)
        }
      }
    }
  }

  fun downloadFile(remotePath: String, localFile: File): VirtualFile {
    val sftpChannel = createSftpChannel()
    sftpChannel.use { it.downloadFileOrDir(remotePath, localFile.absolutePath) }
    return VfsUtil.findFileByIoFile(localFile, true) ?: error("Cannot find downloaded file by path: ${localFile.absolutePath}")
  }

  private fun createSftpChannel(): SftpChannel {
    val sshUiData = SshUiData.create(sshConfig)
    val connectionBuilder = sshUiData.connectionBuilder(project)
    connectionBuilder.withConnectionTimeout(5, TimeUnit.SECONDS)
    return connectionBuilder.openSftpChannel(5)
  }

  private inner class SparSftpUploadProgressAdapter(uploadFile: File,
                                                    private val progressIndicator: RawProgressReporter?,
                                                    val job: Job) : SftpProgressTracker {
    private val totalSize = uploadFile.length()
    var transfered = 0L

    override val isCanceled: Boolean = !job.isActive

    override fun onFileCopied(file: File) {}

    override fun onBytesTransferred(count: Long) {
      job.ensureActive()
      transfered += count
      progressIndicator?.text(
        MessagesBundle.message("upload.progress.text2", SizeUtils.toString(transfered), SizeUtils.toString(totalSize)))
      progressIndicator?.fraction(transfered.toDouble() / totalSize)
    }
  }
}