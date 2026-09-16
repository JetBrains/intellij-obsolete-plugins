package com.intellij.bigdatatools.databricks.rfs.localcache

import com.databricks.sdk.service.workspace.ObjectType
import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils.originInfo
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.localcache.FullTextContentDownloader

class DatabricksWorkspaceContentDownloader : FullTextContentDownloader() {
  override fun accept(fileInfo: FileInfo) = fileInfo.originInfo is DatabricksWorkspaceFileInfo

  override fun contentExtension(fileInfo: FileInfo): String {
    val originInfo = fileInfo.originInfo
    return if (originInfo is DatabricksWorkspaceFileInfo && originInfo.info.objectType == ObjectType.NOTEBOOK) {
      "inote"
    }
    else {
      ""
    }
  }
}