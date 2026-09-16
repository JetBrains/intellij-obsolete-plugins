package com.jetbrains.bigdatatools.dataproc.rfs

import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.ClusterFileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import java.io.InputStream

class DataprocFileInfo(override val driver: DataprocDriver, override val clusterInfo: DataprocClusterInfo) : ClusterFileInfo() {
  override val externalPath: String = clusterInfo.cluster.clusterUuid
  override val path: RfsPath = RfsPath(listOf(clusterInfo.id), isDirectory = true)
  override val length: Long = -1
  override val modificationTime: Long = -1

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