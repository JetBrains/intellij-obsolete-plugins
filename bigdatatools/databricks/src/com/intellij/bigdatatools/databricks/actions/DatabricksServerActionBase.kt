package com.intellij.bigdatatools.databricks.actions

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MainTreeController.Companion.dataManager
import org.jetbrains.annotations.Nls

internal abstract class DatabricksServerActionBase : DumbAwareAction() {

  protected abstract fun performServerAction(dataManager: DatabricksDataManager, cluster: ClusterInfoPresentable)

  protected abstract fun isSkipConfirmation(): Boolean
  protected abstract fun setShowConfirmation(isSelected: Boolean)
  protected abstract fun getConfirmationMessage(cluster: ClusterInfoPresentable): @Nls String

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  override fun actionPerformed(e: AnActionEvent) {
    val dataManager = e.dataManager as? DatabricksDataManager ?: return
    val cluster = dataManager.getCurrentCluster() ?: return

    if (!isSkipConfirmation()) {
      val res = MessageDialogBuilder.Message("", getConfirmationMessage(cluster))
        .buttons(Messages.getOkButton(), Messages.getCancelButton())
        .defaultButton(Messages.getOkButton())
        .doNotAsk(object : DoNotAskOption.Adapter() {
          override fun rememberChoice(isSelected: Boolean, exitCode: Int) = setShowConfirmation(isSelected)
        }).show(e.project) == Messages.getOkButton()

      if (!res)
        return
    }

    performServerAction(dataManager, cluster)
  }
}