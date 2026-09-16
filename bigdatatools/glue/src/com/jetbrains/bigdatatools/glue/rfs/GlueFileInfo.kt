package com.jetbrains.bigdatatools.glue.rfs

import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfoBase
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import software.amazon.awssdk.services.glue.model.Column
import software.amazon.awssdk.services.glue.model.Database
import software.amazon.awssdk.services.glue.model.Table
import java.io.InputStream

class GlueFileInfo(override val driver: GlueDriver,
                   override val path: RfsPath,
                   val database: Database?,
                   val table: Table?,
                   val column: Column?) : FileInfoBase() {
  override val externalPath: String = path.stringRepresentation()
  override val length: Long = -1
  override val modificationTime: Long = -1
  override val isActionDeleteSupport: Boolean = false
  override val isMoveSupport: Boolean = false
  override val isCopySupport: Boolean = false

  override fun isMetaInfoSupport() = !path.isFile

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