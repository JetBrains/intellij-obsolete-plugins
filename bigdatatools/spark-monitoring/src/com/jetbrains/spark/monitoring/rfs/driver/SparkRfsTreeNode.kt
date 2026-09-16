package com.jetbrains.spark.monitoring.rfs.driver

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringRfsTreeNode
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

class SparkRfsTreeNode(
  project: Project,
  rfsPath: RfsPath,
  driver: SparkMonitoringDriver,
) : MonitoringRfsTreeNode(project, rfsPath, driver) {
  init {
    myName = rfsPath.name
  }

  override fun isAlwaysLeaf() = rfsPath.isFile

  override fun onDoubleClick(): Boolean {
    val project = project ?: return true

    MonitoringServiceProvider.getSparkMonitoringService()?.focusOn(project, driver.connectionData, rfsPath.name, false)
    return true
  }

  override fun getGrayText(): String? {
    val sparkFileInfo = fileInfo as? SparkFileInfo ?: return null
    return when {
      sparkFileInfo.duration != null && sparkFileInfo.byMe == true -> sparkFileInfo.duration + " by me"
      sparkFileInfo.duration != null -> sparkFileInfo.duration
      sparkFileInfo.byMe == true -> "by me"
      else -> null
    }
  }

  override fun getIdleIcon() = if (rfsPath.isRoot)
    super.getIdleIcon()
  else
    (fileInfo as? SparkFileInfo)?.statusIcon
}