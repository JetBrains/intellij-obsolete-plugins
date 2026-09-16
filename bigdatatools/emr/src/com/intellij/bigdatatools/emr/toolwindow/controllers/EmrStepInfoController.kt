package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.list.ListClickHelper
import com.jetbrains.bigdatatools.common.monitoring.list.ListValueRenderer
import com.jetbrains.bigdatatools.common.monitoring.list.extension.ListCellPreview
import com.jetbrains.bigdatatools.common.monitoring.list.model.ListTableModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.table.search.SearchAwareMaterialTable
import kotlin.reflect.KProperty1

class EmrStepInfoController(project: Project, override val dataManager: EmrDataManager) :
  AbstractGroupFieldsModelsController<String>(project, dataManager.connectionData.innerId) {

  var clusterId: String = ""

  override val toolWindowSettings = EmrToolWindowSettings.getInstance()

  init {
    init()
  }

  override fun getFieldsGroupModel(id: String) = dataManager.getStepInfoModel(clusterId, id)

  override fun createTable(renderableColumns: List<KProperty1<out Any, *>>,
                           tableModel: ListTableModel): SearchAwareMaterialTable {
    val table = super.createTable(renderableColumns, tableModel)

    val linkRenderer = LinkRenderer().apply {
      condition = { _, value, _, _ -> value?.toString()?.startsWith("s3") == true }
    }

    val rowRenderers = hashMapOf(
      "Log Error File:" to linkRenderer,
      "JAR location:" to linkRenderer,
      EmrMessagesBundle.message("step.info.logs") to linkRenderer,
      EmrMessagesBundle.message("step.info.log.controller") to linkRenderer,
      EmrMessagesBundle.message("step.info.log.syslog") to linkRenderer,
      EmrMessagesBundle.message("step.info.log.stderr") to linkRenderer,
      EmrMessagesBundle.message("step.info.log.stdout") to linkRenderer,
    )

    table.columnModel.getColumn(1).cellRenderer = ListValueRenderer(rowRenderers)

    ListCellPreview.installOn(table, listOf("Details:", "Arguments:"))

    ListClickHelper.installOn(table, "Log Error File:") {
      val url = it as? String ?: return@installOn
      dataManager.createS3LogConnection(project, clusterId, url)

    }

    ListClickHelper.installOn(table, "JAR location:") {
      val url = it as? String ?: return@installOn
      if (url.startsWith("s3"))
        dataManager.createS3LogConnection(project, clusterId, url)
    }

    ListClickHelper.installOn(table, EmrMessagesBundle.message("step.info.logs")) {
      val s3Url = it as? String ?: return@installOn
      dataManager.createS3LogConnection(project, clusterId, s3Url)
    }

    ListClickHelper.installOn(table, EmrMessagesBundle.message("step.info.log.controller")) {
      val url = it as? String ?: return@installOn
      dataManager.openLogFile(project, clusterId, url)
    }

    ListClickHelper.installOn(table, EmrMessagesBundle.message("step.info.log.syslog")) {
      val url = it as? String ?: return@installOn
      dataManager.openLogFile(project, clusterId, url)
    }

    ListClickHelper.installOn(table, EmrMessagesBundle.message("step.info.log.stderr")) {
      val url = it as? String ?: return@installOn
      dataManager.openLogFile(project, clusterId, url)
    }

    ListClickHelper.installOn(table, EmrMessagesBundle.message("step.info.log.stdout")) {
      val url = it as? String ?: return@installOn
      dataManager.openLogFile(project, clusterId, url)
    }

    return table
  }
}