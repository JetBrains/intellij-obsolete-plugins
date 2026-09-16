package com.jetbrains.spark.submit.run.ssh.ui

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.Messages
import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface SshConfigProvider {
  suspend fun getSshConfig(): SshConfig?
}

interface SshFileSelectorContext : FileSelectorContext, SshConfigProvider {
  suspend fun revalidateFormAndGetSshConfig(): SshConfig? {
    val result = getSshConfig()
    if (result == null) {
      withContext(Dispatchers.EDT) {
        Messages.showErrorDialog(project,
                                 SparkMessagesBundle.message("settings.ssh.error.msg"),
                                 SparkMessagesBundle.message("settings.ssh.error.title"))
      }
    }
    return result
  }
}