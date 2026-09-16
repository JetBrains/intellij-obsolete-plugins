package com.jetbrains.bigdatatools.hivemetastore.rfs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder

class HiveMetastoreTreeNodeBuilder : RfsDriverTreeNodeBuilder() {
  override fun createNode(project: Project, path: RfsPath, driver: Driver) = HiveMetastoreTreeNode(project, path,
                                                                                                   driver as HiveMetastoreDriver)
}