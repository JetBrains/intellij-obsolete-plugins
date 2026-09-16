package com.jetbrains.bigdatatools.hivemetastore.rfs.metainfo

import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.LoadComponentController
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.SelectableLabel
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoBlock
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.details.FileInfoDetailsBase
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.rowIfNotBlank
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveFileInfo
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveMetastoreDriver
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.catalog
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.database
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.isCatalog
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.isDatabase
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.isTable
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.table
import org.apache.hadoop.hive.metastore.api.PrincipalPrivilegeSet
import org.jetbrains.annotations.Nls

class HiveMetainfoFileDetails(rfsTreeNode: DriverFileRfsTreeNode, parentDisposable: Disposable) : FileInfoDetailsBase(rfsTreeNode,
                                                                                                                      parentDisposable) {
  private val client = (rfsTreeNode.driver as HiveMetastoreDriver).client

  override fun getBlocks(): List<FileInfoBlock> {
    val panel = MigPanel(UiUtil.insets10FillXHidemode3).apply {
      val path = rfsTreeNode.rfsPath
      path.catalog?.let { row(HiveMessagesBundle.message("meta.label.catalog"), SelectableLabel(it)) }
      path.database?.let { row(HiveMessagesBundle.message("meta.label.database"), SelectableLabel(it)) }
      path.table?.let { row(HiveMessagesBundle.message("meta.label.table"), SelectableLabel(it)) }

      showErrorInfo(rfsTreeNode)

      createCatalogBlock(rfsTreeNode, this@HiveMetainfoFileDetails)
      createDatabaseBlock(rfsTreeNode, this@HiveMetainfoFileDetails)
      createTableBlock(rfsTreeNode, this@HiveMetainfoFileDetails)
      createSchemaBlock(rfsTreeNode)
    }
    return listOf(FileInfoBlock("", panel))
  }

  private fun MigPanel.createCatalogBlock(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) {
    val rfsPath = rfsTreeNode.rfsPath

    if (!rfsPath.isCatalog)
      return

    createCalculatingRow(rfsTreeNode, HiveMessagesBundle.message("meta.title.catalog.info"), curWindowDisposable) { migBlock ->
      val info = client.getCatalogInfo(rfsPath) ?: return@createCalculatingRow false

      migBlock.removeAll()
      migBlock.title(HiveMessagesBundle.message("meta.title.catalog.info"))
      migBlock.row(HiveMessagesBundle.message("meta.label.description"), SelectableLabel(info.description))
      migBlock.row(HiveMessagesBundle.message("meta.label.location.uri"), SelectableLabel(info.locationUri))
      true
    }
  }

  private fun MigPanel.createDatabaseBlock(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) {
    val rfsPath = rfsTreeNode.rfsPath

    if (!rfsPath.isDatabase)
      return

    createCalculatingRow(rfsTreeNode, HiveMessagesBundle.message("meta.database.info"), curWindowDisposable) { migBlock ->
      val info = client.getDatabaseInfo(rfsPath) ?: return@createCalculatingRow false
      migBlock.removeAll()
      migBlock.title(HiveMessagesBundle.message("meta.database.info"))
      migBlock.row(HiveMessagesBundle.message("meta.label.description"), SelectableLabel(info.description))
      migBlock.row(HiveMessagesBundle.message("meta.label.location.uri"), SelectableLabel(info.locationUri))
      migBlock.row(HiveMessagesBundle.message("meta.label.owner"), SelectableLabel(info.ownerName))
      migBlock.row(HiveMessagesBundle.message("meta.label.owner.type"), SelectableLabel(info.ownerType.name))
      paramsBlock(info.parameters, migBlock)
      privilegesBlock(info.privileges, migBlock)

      true
    }
  }

  private fun MigPanel.createTableBlock(rfsTreeNode: DriverFileRfsTreeNode, curWindowDisposable: Disposable) {
    val rfsPath = rfsTreeNode.rfsPath

    if (!rfsPath.isTable)
      return

    createCalculatingRow(rfsTreeNode, HiveMessagesBundle.message("meta.table.info"), curWindowDisposable) { migBlock ->
      val info = client.getTableInfo(rfsPath) ?: return@createCalculatingRow false

      migBlock.apply {
        removeAll()
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.type"), info.tableType)
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.owner"), info.owner)
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.owner.type"), info.ownerType.name)
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.create.time"),
                      if (info.createTime <= 0) null else TimeUtils.unixTimeToString(info.createTime.toLong() * 1000))
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.last.access.time"),
                      if (info.lastAccessTime <= 0) null else TimeUtils.unixTimeToString(info.lastAccessTime.toLong() * 1000))
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.temporary"), info.isTemporary.toString())
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.retention"), info.retention.toString())

        paramsBlock(info.parameters, this)
      }

      title(HiveMessagesBundle.message("meta.separator.storage.description"))
      val sd = info.sd
      migBlock.apply {
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.input.format"), sd.inputFormat)
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.output.format"), sd.outputFormat)
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.location"), sd.location)
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.cols.size"), sd.bucketColsSize.toString())
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.compressed"), sd.isCompressed.toString())
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.num.buckets"), if (sd.numBuckets < 0) null else sd.numBuckets.toString())
        rowIfNotBlank(HiveMessagesBundle.message("meta.label.sort.columns"), sd.sortCols.toString())
      }

      title(HiveMessagesBundle.message("meta.separator.serializer.deserializer"))
      val serdeInfo = sd.serdeInfo
      if (serdeInfo != null) {
        migBlock.apply {
          rowIfNotBlank(HiveMessagesBundle.message("meta.label.name"), serdeInfo.name)
          rowIfNotBlank(HiveMessagesBundle.message("meta.label.type"), serdeInfo.serdeType?.name)
          rowIfNotBlank(HiveMessagesBundle.message("meta.label.description"), serdeInfo.description)
          rowIfNotBlank(HiveMessagesBundle.message("meta.label.library"), serdeInfo.serializationLib)
          rowIfNotBlank(HiveMessagesBundle.message("meta.label.serializer"), serdeInfo.serializerClass)
          rowIfNotBlank(HiveMessagesBundle.message("meta.label.deserializer"), serdeInfo.deserializerClass)
          paramsBlock(serdeInfo.parameters, this)
        }
      }

      privilegesBlock(info.privileges, migBlock)
      true
    }
  }

  private fun MigPanel.createSchemaBlock(rfsTreeNode: DriverFileRfsTreeNode) {
    val schema = (rfsTreeNode.fileInfo as? HiveFileInfo)?.fieldSchema ?: return
    MigBlock(this).apply {
      title(HiveMessagesBundle.message("meta.separator.schema.info"))
      rowIfNotBlank(HiveMessagesBundle.message("meta.label.column"), schema.name)
      rowIfNotBlank(HiveMessagesBundle.message("meta.label.type"), schema.type)
      rowIfNotBlank(HiveMessagesBundle.message("meta.label.comment"), schema.comment)
    }
  }

  private fun MigPanel.createCalculatingRow(rfsTreeNode: DriverFileRfsTreeNode,
                                            @Nls title: String,
                                            curWindowDisposable: Disposable,
                                            innerBlock: (MigBlock) -> Boolean) {
    val project = rfsTreeNode.project ?: return
    val fileInfo = rfsTreeNode.fileInfo ?: return

    val migBlock = MigBlock(this)

    val controller = LoadComponentController(project, fileInfo, title, null, curWindowDisposable, true) {
      val res = innerBlock(migBlock)

      if (!res)
        return@LoadComponentController false

      revalidate()
      repaint()
      true
    }

    migBlock.row("$title:", controller.component)
  }

  private fun MigPanel.privilegesBlock(privileges: PrincipalPrivilegeSet?, target: MigBlock) {
    if (privileges == null)
      return

    title(HiveMessagesBundle.message("meta.separator.privileges"))
    target.row(HiveMessagesBundle.message("meta.label.group.privileges"), SelectableLabel(privileges.groupPrivileges?.toString() ?: ""))
    target.row(HiveMessagesBundle.message("meta.label.user.privileges"), SelectableLabel(privileges.userPrivileges?.toString() ?: ""))
    target.row(HiveMessagesBundle.message("meta.label.role.privileges"), SelectableLabel(privileges.rolePrivileges?.toString() ?: ""))
  }

  private fun paramsBlock(parameters: MutableMap<String, String>?, migBlock: MigBlock) {
    if (parameters.isNullOrEmpty())
      return

    val panel = MigPanel().apply {
      title(HiveMessagesBundle.message("meta.separator.parameters"))
      parameters.entries.forEach {
        rowIfNotBlank(it.key, it.value)
      }
    }
    migBlock.block(panel)
  }
}