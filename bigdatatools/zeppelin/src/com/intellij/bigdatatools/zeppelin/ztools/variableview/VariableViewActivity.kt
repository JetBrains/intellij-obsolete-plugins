package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.ConnectionSettingsListener
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.zeppelin.idea.toolwindow.ZtoolsToolWindowUtils
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager

class VariableViewActivity : ProjectActivity {

  private fun updateStateViewer(project: Project) {
    val zeppelinConnections = RfsConnectionDataManager.instance?.getTyped<ZeppelinConnectionData>(project)

    val hasStateViewer = zeppelinConnections?.any { it.isEnabled && it.isZtoolsEnabled == true } ?: false

    if (!hasStateViewer) {
      ZtoolsToolWindowUtils.hideToolWindow(project)
    }
    else {
      ZtoolsToolWindowUtils.showToolWindowStripeButton(project)
    }
  }

  override suspend fun execute(project: Project) {
    val listener = object : ConnectionSettingsListener {
      private fun updateIfSuitableData(connectionData: ConnectionData) {
        if (connectionData is ZeppelinConnectionData) {
          updateStateViewer(project)
        }
      }

      override fun onConnectionRemoved(project: Project?,
                                       removedConnectionData: ConnectionData) = updateIfSuitableData(removedConnectionData)

      override fun onConnectionAdded(project: Project?,
                                     newConnectionData: ConnectionData) = updateIfSuitableData(newConnectionData)

      override fun onConnectionModified(project: Project?,
                                        connectionData: ConnectionData,
                                        modified: Collection<ModificationKey>) = updateIfSuitableData(connectionData)
    }
    Disposer.register(project) {
      RfsConnectionDataManager.instance?.removeListener(listener)
    }
    RfsConnectionDataManager.instance?.addListener(listener)
    updateStateViewer(project)
  }
}