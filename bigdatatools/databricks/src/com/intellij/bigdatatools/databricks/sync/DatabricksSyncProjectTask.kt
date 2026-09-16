package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.databricks.cli.DatabricksCliManager
import com.intellij.bigdatatools.databricks.cli.DatabricksCliWrapper
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.io.BaseOutputReader
import kotlinx.coroutines.delay

internal class DatabricksSyncProjectTask(val project: Project, private val dataManager: DatabricksDataManager) : Disposable {
  val syncMapper = SyncMapper(project, dataManager)
  val processListener = SyncProcessListener()
  var processHandler: KillableProcessHandler? = null

  val statusObservable
    get() = processListener.status

  val status
    get() = statusObservable.get()


  override fun dispose() {
    processHandler?.killProcess()
  }


  suspend fun start() {
    processHandler?.killProcess()
    val command = createCommand()
    val handler = object : KillableProcessHandler(command) {
      override fun readerOptions(): BaseOutputReader.Options {
        return BaseOutputReader.Options.forMostlySilentProcess()
      }
    }

    processHandler = handler
    processHandler?.addProcessListener(processListener)
    processListener.attach(handler)

    processHandler?.startNotify()
  }

  suspend fun waitForSync(): Boolean {
    while (processListener.status.get() == SyncStatus.IN_PROGRESS) {
      delay(500)
    }
    return processListener.status.get() == SyncStatus.WATCHING_FOR_CHANGES
  }

  private suspend fun createCommand(): GeneralCommandLine {
    val cliPath = DatabricksCliManager.getOrDownloadDatabricksCli(project, calledByUser = true)
    val cliWrapper = DatabricksCliWrapper(cliPath, project)

    val remotePath = syncMapper.baseRemotePath
    return cliWrapper.getSyncCommand(remotePath, dataManager.client.getConfig())
  }

  fun stop() {
    processHandler?.killProcess()
    processHandler = null
  }
}