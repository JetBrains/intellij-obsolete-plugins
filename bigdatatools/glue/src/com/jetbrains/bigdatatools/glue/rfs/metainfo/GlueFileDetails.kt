package com.jetbrains.bigdatatools.glue.rfs.metainfo

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.SelectableLabel
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoBlock
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoDetailsBase
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.rowIfNotBlank
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.glue.rfs.GlueFileInfo
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import com.jetbrains.bigdatatools.glue.utils.GlueUtils.database
import com.jetbrains.bigdatatools.glue.utils.GlueUtils.table
import software.amazon.awssdk.services.glue.model.Column
import software.amazon.awssdk.services.glue.model.Database
import software.amazon.awssdk.services.glue.model.Table

class GlueFileDetails(rfsTreeNode: DriverFileRfsTreeNode, parentDisposable: Disposable) : FileInfoDetailsBase(rfsTreeNode,
                                                                                                              parentDisposable) {

  override fun getBlocks(): List<FileInfoBlock> {
    val panel = MigPanel(UiUtil.insets10FillXHidemode3).apply {
      val path = rfsTreeNode.rfsPath

      val info = rfsTreeNode.fileInfo as? GlueFileInfo ?: return@apply

      rowIfNotBlank(GlueMessagesBundle.message("meta.label.database"), path.database)
      rowIfNotBlank(GlueMessagesBundle.message("meta.label.table"), path.table)

      showErrorInfo(rfsTreeNode)

      info.database?.let { createDatabaseBlock(it) }
      info.table?.let { createTableBlock(it) }
      info.column?.let { createSchemaBlock(it) }
    }

    return listOf(FileInfoBlock("", panel))
  }

  private fun MigPanel.createDatabaseBlock(database: Database) {
    title(GlueMessagesBundle.message("meta.database.info"))
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.description"), database.description())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.location.uri"), database.locationUri())
    paramsBlock(database.parameters(), this)
  }

  private fun MigPanel.createTableBlock(info: Table) {
    removeAll()
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.type"), info.tableType())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.owner"), info.owner())

    if (info.createTime() != null && info.createTime().toEpochMilli() > 0)
      rowIfNotBlank(GlueMessagesBundle.message("meta.label.create.time"),
                    TimeUtils.unixTimeToString(info.createTime().toEpochMilli()))
    if (info.lastAccessTime() != null && info.lastAccessTime().toEpochMilli() > 0)
      rowIfNotBlank(GlueMessagesBundle.message("meta.label.last.access.time"),
                    TimeUtils.unixTimeToString(info.lastAccessTime().toEpochMilli()))
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.retention"), info.retention()?.toString())

    paramsBlock(info.parameters(), this)

    title(GlueMessagesBundle.message("meta.separator.storage.description"))
    val sd = info.storageDescriptor()
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.input.format"), sd.inputFormat())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.output.format"), sd.outputFormat())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.location"), sd.location())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.cols.size"), sd.columns().size.toString())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.compressed"), sd.compressed().toString())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.num.buckets"),
                  if (sd.numberOfBuckets() < 0) null else sd.numberOfBuckets().toString())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.sort.columns"), sd.sortColumns().toString())

    title(GlueMessagesBundle.message("meta.separator.serializer.deserializer"))
    val serdeInfo = sd.serdeInfo()
    if (serdeInfo != null) {
      rowIfNotBlank(GlueMessagesBundle.message("meta.label.name"), serdeInfo.name())
      rowIfNotBlank(GlueMessagesBundle.message("meta.label.library"), serdeInfo.serializationLibrary())
      paramsBlock(serdeInfo.parameters(), this)
    }
  }

  private fun MigPanel.createSchemaBlock(column: Column) {
    title(GlueMessagesBundle.message("meta.separator.schema.info"))
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.column"), column.name())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.type"), column.type())
    rowIfNotBlank(GlueMessagesBundle.message("meta.label.comment"), column.comment())
  }

  private fun paramsBlock(parameters: MutableMap<String, String>?, migBlock: MigPanel) {
    if (parameters.isNullOrEmpty())
      return

    parameters.entries.forEach {
      migBlock.row(it.key, SelectableLabel(it.value))
    }
  }
}