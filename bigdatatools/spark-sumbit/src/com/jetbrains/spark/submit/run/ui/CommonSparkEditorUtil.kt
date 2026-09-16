package com.jetbrains.spark.submit.run.ui

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.util.SparkJobClassScanner
import com.jetbrains.spark.submit.util.SparkMessagesBundle

object CommonSparkEditorUtil {
  fun getFoundClasses(project: Project, artifactPath: FilePath, resolveArtifact: (ProgressIndicator) -> String?): List<String> {
    return try {
      runWithModalProgressBlocking(project, SparkMessagesBundle.message("receive.artifact.task")) {
        coroutineToIndicator {
          val indicator = ProgressManager.getGlobalProgressIndicator()
          indicator.text = artifactPath.toString()
          val path = resolveArtifact(indicator) ?: return@coroutineToIndicator emptyList<String>()
          findMainClassesInJar(path, indicator)
        }
      }
    }
    catch (t: Throwable) {
      NotificationUtils.showExceptionMessage(project, t)
      emptyList()
    }
  }

  private fun findMainClassesInJar(jarPath: String, indicator: ProgressIndicator? = null): List<String> {
    if (jarPath.isEmpty())
      return emptyList()

    val fileUrl = VfsUtilCore.pathToUrl(jarPath)
    val file = VirtualFileManager.getInstance().refreshAndFindFileByUrl(fileUrl) ?: return emptyList()
    if (file.extension != "jar")
      return emptyList()
    val scanner = SparkJobClassScanner()
    return scanner.scanClasses(file, indicator).filter { !it.endsWith("$") }.sorted()
  }

  fun selectClassName(project: Project, classes: List<String>, selectedClass: String?): String? {
    val dialog = SelectClassDialog(project, selectedClass, classes)
    return if (dialog.show()) dialog.selectedClass else null
  }
}