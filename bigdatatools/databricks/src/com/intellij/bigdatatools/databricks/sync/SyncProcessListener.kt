package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.databricks.statistic.DatabricksFeatureCollector
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.util.Key
import com.intellij.bigdatatools.coreUi.serializer.BdtJson

internal class SyncProcessListener : ProcessAdapter() {
  val status = AtomicProperty(SyncStatus.STOPPED)
  val statusMessage = AtomicProperty(DatabricksBundle.message("sync.status.stopped"))
  private var curProcess: ProcessHandler? = null

  init {
    status.afterChange {
      DatabricksFeatureCollector.syncDirectoryUpdated(status.get())
    }

  }
  fun attach(process: ProcessHandler) {
    curProcess = process
  }

  override fun startNotified(event: ProcessEvent) {
    if (event.processHandler != curProcess)
      return

    status.set(SyncStatus.IN_PROGRESS)
    statusMessage.set(DatabricksBundle.message("sync.status.init"))
    super.startNotified(event)
  }

  override fun processTerminated(event: ProcessEvent) {
    if (event.processHandler != curProcess)
      return

    if (status.get() in setOf(SyncStatus.IN_PROGRESS, SyncStatus.WATCHING_FOR_CHANGES)) {
      status.set(SyncStatus.STOPPED)
      statusMessage.set(DatabricksBundle.message("sync.status.aborted"))
    }

    super.processTerminated(event)
  }

  override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
    if (event.processHandler != curProcess)
      return
  }

  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
    if (event.processHandler != curProcess)
      return

    val text = event.text
    if (ProcessOutputType.isStdout(outputType)) {
      val syncEvent = try {
        BdtJson.fromJsonToClass(text, DatabricksCliSyncEvent::class.java)
      }
      catch (t: Throwable) {
        thisLogger().warn("Cannot parse $text", t)
        return
      }
      status.set(syncEvent.getStatus())
      statusMessage.set(syncEvent.getStatusMessage())
    }
    else if (ProcessOutputType.isStderr(outputType)) {
      if (matchForErrors(text)) {
        curProcess?.destroyProcess()
      }
    }
  }

  private fun matchForErrors(line: String): Boolean {
    val filesInWorkspaceDisabledPattern = "^Error: .*Files in Workspace is disabled.*".toRegex()
    val filesInReposDisabledPattern = "^Error: .*Files in Repos is disabled.*".toRegex()
    val fileAlreadyExists = "^Error: file already exists:*".toRegex()
    val errorPattern = "^Error: (.*)".toRegex()

    return when {
      filesInWorkspaceDisabledPattern.containsMatchIn(line) -> {
        status.set(SyncStatus.ERROR)
        statusMessage.set(DatabricksBundle.message("sync.status.files.in.workspace.disabled"))
        true
      }
      filesInReposDisabledPattern.containsMatchIn(line) -> {
        status.set(SyncStatus.ERROR)
        statusMessage.set(DatabricksBundle.message("sync.status.files.in.repos.disabled"))
        true
      }
      fileAlreadyExists.containsMatchIn(line) -> {
        status.set(SyncStatus.ERROR)
        val error = errorPattern.find(line)?.groupValues?.getOrNull(1) ?: line
        statusMessage.set(error+" "+ DatabricksBundle.message("sync.status.already.exists"))
        //Ignore this waring
        false
      }

      else -> {
        val match = errorPattern.find(line)
        if (match != null) {
          status.set(SyncStatus.ERROR)
          statusMessage.set(match.groupValues[1])
          return true
        }
        false
      }
    }
  }
}