package com.intellij.bigdatatools.databricks.rfs

import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfoBase
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsDeleteTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import java.io.InputStream

internal class DatabricksFileInfo(override val driver: DatabricksDriver, override val path: RfsPath) : FileInfoBase() {
  override val externalPath: String = path.stringRepresentation()
  override val length: Long = -1
  override val modificationTime: Long = -1

  override val isCopySupport: Boolean = false
  override val isActionDeleteSupport: Boolean = false
  override val isMoveSupport: Boolean = false

  override fun isMetaInfoSupport(): Boolean = false

  override fun doDeleteAsync() = object : RemoteFsDeleteTask(path) {
    override fun run(indicator: ProgressIndicator) {
      //when (path.parent) {
      //  DatabricksDriver.schemasPath -> runBlockingCancellable {
      //    driver.dataManager.deleteSchema(path.name).join()
      //  }
      //}
    }
  }

  override fun doRenameAsync(newPath: RfsPath, overwrite: Boolean): RfsCopyMoveTask {
    TODO("Not yet implemented")
  }

  override fun doGetReadStream(offset: Long, exportFormat: ExportFormat?): InputStream {
    TODO("Not yet implemented")
  }
}