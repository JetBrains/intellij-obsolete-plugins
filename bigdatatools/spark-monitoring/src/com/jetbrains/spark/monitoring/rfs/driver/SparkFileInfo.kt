package com.jetbrains.spark.monitoring.rfs.driver

import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfoBase
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.ui.utils.IconUtils
import java.io.InputStream
import javax.swing.Icon

class SparkFileInfo(override val driver: SparkMonitoringDriver,
                    override val path: RfsPath,
                    appInfo: PresentableApplicationInfo?,
                    val byMe: Boolean? = false) : FileInfoBase() {
  val statusIcon: Icon? = appInfo?.status?.let { IconUtils.getIconForApplicationStatus(it) }
  val duration: String? = appInfo?.duration?.takeIf { it > 0 }?.let { TimeUtils.intervalAsString(it, withMs = false) }

  override val externalPath: String = path.stringRepresentation()
  override val length: Long = -1
  override val modificationTime: Long = -1

  override val isCopySupport: Boolean = false
  override val isActionDeleteSupport: Boolean = false
  override val isMoveSupport: Boolean = false

  override fun isMetaInfoSupport(): Boolean = false

  override fun doDeleteAsync(): RemoteFsTask {
    throw UnsupportedOperationException()
  }

  override fun doRenameAsync(newPath: RfsPath, overwrite: Boolean): RfsCopyMoveTask {
    throw UnsupportedOperationException()
  }

  override fun doGetReadStream(offset: Long, exportFormat: ExportFormat?): InputStream {
    throw UnsupportedOperationException()
  }
}