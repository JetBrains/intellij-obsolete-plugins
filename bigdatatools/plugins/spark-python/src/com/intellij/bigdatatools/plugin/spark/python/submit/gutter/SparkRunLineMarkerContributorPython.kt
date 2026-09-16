package com.intellij.bigdatatools.plugin.spark.python.submit.gutter

import com.intellij.bigdatatools.plugin.spark.python.submit.PySparkSubmitConfigurationType
import com.intellij.bigdatatools.plugin.spark.submit.gutter.SparkRunLineMarkerContributor
import com.intellij.bigdatatools.plugin.spark.textIs
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.cluster.ClusterSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.run.cluster.UploadExistingSelectedArtifactInfo

open class SparkRunLineMarkerContributorPython : SparkRunLineMarkerContributor() {
  override fun shouldShowMarker(element: PsiElement): Boolean {
    if (element.parent !is PyReferenceExpression) return false
    if (!element.textIs("getOrCreate")) return false
    val references = PsiReferenceService.getService().getReferences(element.parent, PsiReferenceService.Hints.NO_HINTS)
    var resolved = false
    for (reference in references) {
      val target = reference.resolve()
      if (target != null) {
        resolved = true
        if (target.containingFile.name.startsWith("session")) return true
      }
    }
    if (!resolved && references.isNotEmpty()) {
      var expression = element.parent
      while (true) {
        expression = when (expression) {
          is PyQualifiedExpression -> {
            val qualifier = expression.qualifier
            if (qualifier == null && expression.textIs("SparkSession")) {
              return true
            }
            qualifier
          }
          is PyCallExpression -> {
            expression.callee
          }
          else -> break
        }
      }
    }
    return false
  }
  override fun findMainClass(element: PsiElement) = null

  override fun suggestedName(element: PsiElement): String? {
    return element.containingFile.virtualFile.nameWithoutExtension
  }

  override fun guessArtifactInfo(element: PsiElement): SelectedArtifactInfo? {
    val file = element.containingFile ?: return null
    if (!file.isPhysical) return null
    if (file.virtualFile.extension != "py") return null
    return UploadExistingSelectedArtifactInfo(file.virtualFile.path)
  }

  override val configurationFactory: ClusterSparkSubmitConfigurationFactory
    get() = runConfigurationType<PySparkSubmitConfigurationType>().clusterFactory

  override fun isFromContext(element: PsiElement,
                             configuration: ClusterSparkJobRunConfiguration): Boolean {
    val file = element.containingFile ?: return false
    return configuration.getAllPaths().any { it.type == FileType.UPLOAD && it.path == file.virtualFile.path }
  }
}