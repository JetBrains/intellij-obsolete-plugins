package com.jetbrains.bigdatatools.glue.rfs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder

class GlueTreeNodeBuilder : RfsDriverTreeNodeBuilder() {
  override fun createNode(project: Project, path: RfsPath, driver: Driver) = GlueTreeNode(project, path,
                                                                                          driver as GlueDriver)
}