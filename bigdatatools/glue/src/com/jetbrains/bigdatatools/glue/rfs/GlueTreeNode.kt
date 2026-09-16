package com.jetbrains.bigdatatools.glue.rfs

import com.jetbrains.bigdatatools.common.BigdatatoolsCoreIcons
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode
import com.jetbrains.bigdatatools.glue.utils.GlueUtils.isSchema
import com.jetbrains.bigdatatools.glue.utils.GlueUtils.isTable

class GlueTreeNode(project: Project,
                   rfsPath: RfsPath,
                   driver: GlueDriver) : DriverFileRfsTreeNode(project, rfsPath, driver) {
  private val focusId = driver.connectionData.innerId

  override fun onDoubleClick(): Boolean {
    if (rfsPath.isRoot) {
      val project = project ?: return true
      val controller = (driver as GlueDriver).getController(project)
      controller?.focusOn(focusId)
      return false
    }

    return true
  }

  override fun getIdleIcon() = when {
    rfsPath.isSchema -> AllIcons.Nodes.Field
    rfsPath.isTable -> RfsIcons.META_TABLE_ICON
    rfsPath.isDirectory -> BigdatatoolsCoreIcons.Nodes.Databases
    else -> super.getIdleIcon()
  }
}