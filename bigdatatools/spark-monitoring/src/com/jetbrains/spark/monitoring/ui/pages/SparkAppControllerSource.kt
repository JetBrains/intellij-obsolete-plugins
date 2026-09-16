package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo

interface SparkAppControllerSource {
  val rightToolbarActions: List<AnAction>
  fun open(project: Project, info: PresentableApplicationInfo)
}