package com.intellij.bigdatatools.plugin.spark.services.actions

import com.intellij.bigdatatools.plugin.spark.services.node.BdtDriverNode
import com.intellij.bigdatatools.plugin.spark.services.node.SparkJobRootNode
import com.intellij.execution.services.ServiceViewActionUtils
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData

abstract class ServiceConnectionAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFromTree(e)
  }

  protected fun getSelectedConnectionIds(e: AnActionEvent): List<String> {
    return getSelectedConnection(e).map { it.innerId }
  }

  protected fun getSelectedConnection(e: AnActionEvent): List<ConnectionData> {
    val items = ServiceViewActionUtils.getTargets(e, BdtDriverNode::class.java)
    return items.map { it.connData }
  }


  fun isFromTree(e: AnActionEvent) = e.getData(ServiceViewActionUtils.IS_FROM_TREE_KEY) == true

  protected fun isRootDriver(e: AnActionEvent) =
    ServiceViewActionUtils.getTargets(e, BdtDriverNode::class.java).all { it.parent is SparkJobRootNode }
}