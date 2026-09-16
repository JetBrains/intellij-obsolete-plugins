package com.intellij.bigdatatools.zeppelin.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

object ZeppelinProjectUtil {
  fun getProjectList(curProject: Project?) = if (curProject != null)
    listOf(curProject)
  else
    ProjectManager.getInstance().openProjects.toList()
}