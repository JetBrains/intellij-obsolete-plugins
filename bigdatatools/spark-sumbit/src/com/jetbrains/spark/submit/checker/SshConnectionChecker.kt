package com.jetbrains.spark.submit.checker

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.createProgressPipe
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.ui.AnimatedIcon
import com.jetbrains.bigdatatools.common.util.BdtSshUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon

abstract class SshConnectionChecker(val coroutineScope: CoroutineScope, val project: Project) {
  private val runningCheckConnectionTask: AtomicReference<Job?> = AtomicReference(null)

  abstract suspend fun getSshConfig(): SshConfig?
  abstract fun updateLinkText(linkText: String)
  abstract fun updateStatusText(text: String)
  abstract fun updateStatusIcon(icon: Icon?)


  fun invoke() {
    runningCheckConnectionTask.getAndSet(coroutineScope.launch(ModalityState.current().asContextElement()) {
      val sshConfig = getSshConfig()
      if (sshConfig == null) {
        updateStatusIcon(AllIcons.General.Error)
        updateStatusText(SparkMessagesBundle.message("check.ssh.connection.ssh.is.not.defined"))
        return@launch
      }
      val sshDetailsLinkCaption = sshConfig.name
      withContext(Dispatchers.EDT) {
        updateLinkText(sshDetailsLinkCaption)
      }
      val pipe = createProgressPipe()
      val collector = launch(Dispatchers.EDT) {
        pipe.progressUpdates().collect { state ->
          updateStatusText(state.text.orEmpty())
        }
      }
      try {
        pipe.collectProgressUpdates {
          updateStatusIcon(AnimatedIcon.Default())
          try {
            val sshUiData = SshUiData.create(sshConfig)
            val (success, message) = BdtSshUtils.testConnectionAndWrapResult(sshUiData, project)
            withContext(Dispatchers.EDT) {
              updateStatusText(message)
              updateStatusIcon(if (success) AllIcons.General.InspectionsOK else AllIcons.General.Error)
            }
          }
          catch (e: CancellationException) {
            withContext(Dispatchers.EDT) {
              updateStatusText("")
              updateStatusIcon(null)
            }
            throw e
          }
          catch (e: Throwable) {
            thisLogger().error(e)
            withContext(Dispatchers.EDT) {
              updateStatusText(MessagesBundle.message("unexpected.error"))
              updateStatusIcon(AllIcons.General.Error)
            }
          }
        }
      }
      finally {
        collector.cancel()
      }
    })?.cancel()
  }

  fun cancel() {
    runningCheckConnectionTask.get()?.cancel()
  }
}