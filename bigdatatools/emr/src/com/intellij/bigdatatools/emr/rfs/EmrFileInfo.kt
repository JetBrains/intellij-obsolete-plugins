package com.intellij.bigdatatools.emr.rfs

import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.rfs.EmrDriver.Companion.isCluster
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.ClusterFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import java.io.InputStream

class EmrFileInfo(override val driver: EmrDriver,
                  override val clusterInfo: EmrClusterInfo,
                  emrClusterAppInfo: BdtConnectionType?) : ClusterFileInfo() {
  override val externalPath: String = clusterInfo.origin.clusterArn() + (emrClusterAppInfo?.name ?: "")

  override val path: RfsPath = RfsPath(
    if (emrClusterAppInfo == null) listOf(clusterInfo.id) else listOf(clusterInfo.id, emrClusterAppInfo.connName),
    isDirectory = emrClusterAppInfo == null)
  override val length: Long = -1
  override val modificationTime: Long = -1

  override fun isMetaInfoSupport() = path.isCluster


  override val isCopySupport: Boolean = false
  override val isActionDeleteSupport: Boolean = false
  override val isMoveSupport: Boolean = false

  override fun doRenameAsync(newPath: RfsPath, overwrite: Boolean): RfsCopyMoveTask {
    TODO("Not yet implemented")
  }

  override fun doDeleteAsync(): RemoteFsTask {
    TODO("Not yet implemented")
  }

  override fun doGetReadStream(offset: Long, exportFormat: ExportFormat?): InputStream {
    TODO("Not yet implemented")
  }
}