package com.intellij.bigdatatools.plugin.spark.java.submit.gutter

import com.intellij.bigdatatools.plugin.spark.java.submit.JvmSparkSubmitConfigurationType
import com.intellij.bigdatatools.plugin.spark.submit.gutter.SparkRunLineMarkerContributor
import com.intellij.bigdatatools.plugin.spark.textIs
import com.intellij.execution.JavaExecutionUtil
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.psi.util.parentOfType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.cluster.ClusterSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.run.cluster.FileSelectorInitializer

open class SparkRunLineMarkerContributorJvm : SparkRunLineMarkerContributor() {
  override fun shouldShowMarker(element: PsiElement): Boolean {
    if (!JavaLibraryUtil.hasLibraryClass(element.project, "org.apache.spark.sql.SparkSession")) return false
    if (!element.textIs("getOrCreate")) return false
    val references = PsiReferenceService.getService().getReferences(element.parent, PsiReferenceService.Hints.NO_HINTS)
    return references.any {
      val target = it.resolve()
      target != null && target.containingFile.name.startsWith("SparkSession")
    }
  }

  override fun findMainClass(element: PsiElement): String? {
    val containingClass = element.parentOfType<PsiClass>()
    return containingClass?.qualifiedName?.takeIf {
      PsiMethodUtil.hasMainMethod(containingClass)
    }
  }

  override fun suggestedName(element: PsiElement): String? {
    return JavaExecutionUtil.getPresentableClassName(findMainClass(element))
  }

  override fun guessArtifactInfo(element: PsiElement): SelectedArtifactInfo? {
    val project = element.project
    val file = element.containingFile ?: return null
    return FileSelectorInitializer.getAll().firstNotNullOfOrNull { it.guessFromContext(project, file) }
  }

  override val configurationFactory: ClusterSparkSubmitConfigurationFactory
    get() = runConfigurationType<JvmSparkSubmitConfigurationType>().clusterFactory

  override fun isFromContext(
    element: PsiElement,
    configuration: ClusterSparkJobRunConfiguration,
  ): Boolean {
    return configuration.className == findMainClass(element)
  }
}