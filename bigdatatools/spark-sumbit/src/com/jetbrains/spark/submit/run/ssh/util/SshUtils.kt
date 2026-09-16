package com.jetbrains.spark.submit.run.ssh.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.forEachWithProgress
import com.intellij.ssh.ConnectionBuilder
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.connectionBuilder
import com.intellij.ssh.ui.unified.SshUiData
import com.jetbrains.spark.submit.run.ssh.service.SparkSfpService
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import java.io.File
import kotlin.math.abs

internal object SshUtils {
  private const val HOME_PATH = "\$HOME"

  fun getConnectionBuilder(project: Project, sshConfig: SshConfig): ConnectionBuilder {
    val sshUiData = SshUiData.create(sshConfig)
    return sshUiData.connectionBuilder(project).withConnectionTimeout(5)
  }

  fun prepareTargetDir(targetDirectory: String,
                       sshConfig: SshConfig? = null) = if (targetDirectory.isBlank() || targetDirectory.removeSuffix("/") == HOME_PATH)
    sshConfig?.let { "/home/${it.username}/" } ?: "$HOME_PATH/"
  else
    targetDirectory


  suspend fun uploadFiles(project: Project, sshConfig: SshConfig, files: List<String>,
                          targetDirectory: String, override: Boolean = false) {
    val sparkSfpService = SparkSfpService(project, sshConfig)

    val targetDirectoryInfo = sparkSfpService.getInfo(targetDirectory)
    if (!targetDirectoryInfo.exists()) {
      error(SparkMessagesBundle.message("upload.target.dir.is.not.found", targetDirectory))
    }

    try {
      if (override) {
        files.forEachWithProgress {
          sparkSfpService.uploadFile(it, targetDirectory)
        }
        return
      }

      files.forEachWithProgress { filePath ->
        val file = File(filePath)
        val info = sparkSfpService.getInfo(targetDirectory + "/" + file.name)
        if (info.exists() && info.size() == file.length() && abs(info.getLastModifiedTime() - file.lastModified() / 1000) < 2000) {
          return@forEachWithProgress
        }
        sparkSfpService.uploadFile(filePath, targetDirectory)
      }
    }
    finally {
      Disposer.dispose(sparkSfpService)
    }
  }

  fun downloadFileToTempDirSync(project: Project, sshConfig: SshConfig, path: String): VirtualFile {
    val sparkSfpService = SparkSfpService(project, sshConfig)
    return sparkSfpService.downloadFile(path, FileUtil.createTempFile("spark-artifact", ".jar"))
  }
}