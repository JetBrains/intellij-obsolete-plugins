package com.intellij.bigdatatools.zeppelin.rfs.node

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.tree.node.DriverFileRfsTreeNode

class ZeppelinRfsTreeNode(project: Project,
                          val zepRfsPath: ZeppelinRfsPath,
                          val zepDriver: ZeppelinDriver) : DriverFileRfsTreeNode(project, zepRfsPath, zepDriver) {
  override fun update(presentation: PresentationData) {
    super.update(presentation)
    if (zepRfsPath.isTrash)
      presentation.presentableText = zepRfsPath.name.removePrefix("~")
  }

  override fun getIdleIcon() = when {
    zepRfsPath.isTrash -> ZeppelinIcons.TRASH_ICON
    zepRfsPath.isFile -> ZeppelinIcons.ZEPPELIN_FILE
    zepRfsPath.isDirectory -> RfsIcons.DIRECTORY_ICON
    else -> super.getIdleIcon()
  }
}