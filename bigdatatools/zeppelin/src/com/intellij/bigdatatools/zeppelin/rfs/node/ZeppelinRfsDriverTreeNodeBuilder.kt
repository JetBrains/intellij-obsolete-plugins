package com.intellij.bigdatatools.zeppelin.rfs.node

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder

class ZeppelinRfsDriverTreeNodeBuilder : RfsDriverTreeNodeBuilder() {
  override fun createNode(project: Project, path: RfsPath, driver: Driver) =
    ZeppelinRfsTreeNode(project, ZeppelinRfsPath.fromRfsPath(path), driver as ZeppelinDriver)
}