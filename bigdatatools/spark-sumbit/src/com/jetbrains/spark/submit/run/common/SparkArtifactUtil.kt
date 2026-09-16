package com.jetbrains.spark.submit.run.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.packaging.artifacts.ArtifactManager
import com.jetbrains.spark.submit.util.SparkSubmitSupportUtils
import org.jetbrains.annotations.NonNls

object SparkArtifactUtil {
  fun getArtifactOut(project: Project, artifactName: String): @NonNls String? {
    if (!SparkSubmitSupportUtils.isArtifactSupported())
      return null
    return ApplicationManager.getApplication().runReadAction(Computable {
      ArtifactManager.getInstance(project).findArtifact(artifactName)?.outputFilePath
    })
  }
}