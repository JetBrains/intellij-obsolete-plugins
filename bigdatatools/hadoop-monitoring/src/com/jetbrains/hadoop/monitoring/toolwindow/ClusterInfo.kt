package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.hadoop.monitoring.action.OpenUrlAction
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ClusterInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings

class ClusterInfo(project: Project, val connectionData: HadoopConnectionData) : AbstractGroupFieldsModelsController<ClusterInfo>(project,
                                                                                                                                 connectionData.innerId) {
  override val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project)!!

  override val toolWindowSettings = HadoopSettings.getInstance()

  init {
    init()
    setDetailsId(ClusterInfo())
  }

  override fun createActions(): List<AnAction> {
    return super.createActions() + listOf(OpenUrlAction({ "cluster/cluster" }, project, dataManager))
  }

  override fun getFieldsGroupModel(id: ClusterInfo): FieldsGroupModel<*> = dataManager.getClusterInfo()
}