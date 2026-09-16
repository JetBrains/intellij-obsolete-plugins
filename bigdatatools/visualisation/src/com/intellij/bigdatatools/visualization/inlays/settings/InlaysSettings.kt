package com.intellij.bigdatatools.visualization.inlays.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

@State(name = "InlaysSettings", storages = [Storage("InlaysSettings.xml")])
class InlaysSettings : PersistentStateComponent<InlaysSettings> {

  companion object {
    fun getInstance(): InlaysSettings = service()
  }

  var imagesExportPath: String? = null
  var tableExportPath: String? = null
  var chartExportPath: String? = null
  var textExportPath: String? = null
  var htmlExportPath: String? = null

  var bokehScriptsPath: String? = null

  override fun getState(): InlaysSettings {
    return this
  }

  override fun loadState(state: InlaysSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  private fun getExportPathByPriority(): String? {
    return tableExportPath ?: textExportPath ?: htmlExportPath ?: imagesExportPath ?: chartExportPath
  }

  private fun getProjectBasePath(): String? {
    return ProjectManager.getInstance().openProjects.firstOrNull()?.basePath
  }

  fun getChartExportPath(project: Project): Path {
    val result = chartExportPath ?: getExportPathByPriority() ?: return projectBasedPath(project, "chart.png")
    return fixPathExtension(result, "chart.png", listOf("png"))
  }

  fun getTableExportPath(project: Project): Path {
    val result = tableExportPath ?: getExportPathByPriority() ?: return projectBasedPath(project, "table.csv")
    return fixPathExtension(result, "table.csv", listOf("csv"))
  }

  fun getImageExportPath(extensions: List<String>): Path {
    val result = imagesExportPath ?: getExportPathByPriority() ?: return projectBasedPath("output.${extensions.first()}")
    return fixPathExtension(result, "output.${extensions.first()}", extensions)
  }

  fun getTextExportPath(project: Project): Path {
    val result = textExportPath ?: getExportPathByPriority() ?: return projectBasedPath(project, "output.txt")
    return fixPathExtension(result, "output.txt", listOf("txt"))
  }

  fun getHtmlsExportPath(): Path {
    val result = htmlExportPath ?: getExportPathByPriority() ?: return projectBasedPath("output.html")
    return fixPathExtension(result, "output.html", listOf("html"))
  }

  private fun projectBasedPath(project: Project, fileName: String): Path {
    return Paths.get((project.basePath ?: File(".").canonicalPath), fileName)
  }

  private fun projectBasedPath(fileName: String): Path {
    return Paths.get((getProjectBasePath() ?: File(".").canonicalPath), fileName)
  }

  private fun fixPathExtension(path: String, defaultFile: String, extensions: List<String>): Path {
    val resultPath = Paths.get(path)
    val finalPath = if (resultPath.isDirectory()) {
      Paths.get(path, defaultFile).absolutePathString()
    }
    else {
      if (extensions.contains(resultPath.extension)) {
        resultPath.absolutePathString()
      }
      else {
        resultPath.resolveSibling(
          resultPath.fileName.toString().removeSuffix(resultPath.extension) + extensions.first()).absolutePathString()
      }
    }
    return Paths.get(finalPath)
  }
}