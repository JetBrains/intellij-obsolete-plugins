package com.intellij.bigdatatools.plugin.spark.services

import com.intellij.bigdatatools.plugin.spark.services.node.SparkJobRootNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

@Service(Service.Level.PROJECT)
class SparkJobRootNodeService(project: Project) : Disposable {
  val rootNode = SparkJobRootNode(project).also { Disposer.register(this, it) }

  override fun dispose() {}

  object Utils {
    fun getInstance(project: Project) = project.service<SparkJobRootNodeService>()
  }
}