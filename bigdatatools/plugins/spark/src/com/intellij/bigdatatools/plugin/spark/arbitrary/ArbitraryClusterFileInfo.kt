package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfoBase
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import java.io.InputStream

class ArbitraryClusterFileInfo(override val driver: ArbitraryClusterDriver, val bdtConnectionType: BdtConnectionType?) : FileInfoBase() {
  override val externalPath: String = bdtConnectionType?.connName ?: ""

  override val path: RfsPath = if (bdtConnectionType != null) {
    RfsPath(listOf(bdtConnectionType.connName), isDirectory = false)
  }
  else {
    RfsPath(listOf(), isDirectory = true)
  }
  override val length: Long = -1
  override val modificationTime: Long = -1

  override val name: String
    get() = bdtConnectionType?.connName ?: ""

  override val isCopySupport: Boolean = false
  override val isActionDeleteSupport: Boolean = false
  override val isMoveSupport: Boolean = false

  override fun isMetaInfoSupport(): Boolean = false
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