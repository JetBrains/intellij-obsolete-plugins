package com.intellij.bigdatatools.plugin.spark.submit.gutter

import com.intellij.bigdatatools.sparkSubmit.icons.BigdatatoolsSparkSubmitIcons
import com.intellij.execution.Location
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.ActionPopupOptions
import com.intellij.ui.popup.PopupFactoryImpl.ActionGroupPopup
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.cluster.ClusterSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import java.awt.event.MouseEvent

interface SparkConfigurationProducer {
  fun createConfiguration(): RunnerAndConfigurationSettings
  fun isFromContext(configuration: ClusterSparkJobRunConfiguration): Boolean
}

interface SparkConfigurationProducerProvider {
  fun sparkConfigurationProducer(element: PsiElement): SparkConfigurationProducer?
  companion object {
    val EXTENSION: LanguageExtension<SparkConfigurationProducerProvider> =
      LanguageExtension("com.intellij.bigdatatools.sparkConfigurationProducerProvider")

    fun findSparkConfigurationProducer(location: Location<*>): SparkConfigurationProducer? {
      val psiElement = location.psiElement
      val sparkConfigurationProducerProviders = EXTENSION.allForLanguageOrAny(psiElement.language)
      return sparkConfigurationProducerProviders.firstNotNullOfOrNull { it.sparkConfigurationProducer(psiElement) }
    }
  }
}

/**
 * each implementor should be registered both as [RunLineMarkerContributor] and [SparkConfigurationProducerProvider]
 */
abstract class SparkRunLineMarkerContributor : RunLineMarkerContributor(), SparkConfigurationProducerProvider {

  override fun getInfo(element: PsiElement) = null

  protected abstract fun shouldShowMarker(element: PsiElement): Boolean

  protected abstract fun findMainClass(element: PsiElement): String?

  protected abstract fun suggestedName(element: PsiElement): String?

  protected abstract fun guessArtifactInfo(element: PsiElement): SelectedArtifactInfo?

  protected fun createConfiguration(element: PsiElement): RunnerAndConfigurationSettings {
    val project = element.project
    val name = RunManager.getInstance(project).suggestUniqueName(suggestedName(element), configurationFactory.type)
    val configurationSettings = RunManager.getInstance(element.project).createConfiguration(name, configurationFactory)
    val configuration = configurationSettings.configuration as ClusterSparkJobRunConfiguration

    configuration.className = findMainClass(element).orEmpty()
    val selectedArtifactInfo = guessArtifactInfo(element)
    if (selectedArtifactInfo != null) {
      configuration.selectedArtifactInfo = selectedArtifactInfo
      configuration.beforeRunTasks = selectedArtifactInfo.createBeforeTasks(configuration.beforeRunTasks)
      configuration.artifactPath = selectedArtifactInfo.filePath
    }
    return configurationSettings
  }

  protected abstract fun isFromContext(element: PsiElement, configuration: ClusterSparkJobRunConfiguration): Boolean

  protected abstract val configurationFactory: ClusterSparkSubmitConfigurationFactory

  override fun sparkConfigurationProducer(element: PsiElement): SparkConfigurationProducer? {
    if (!shouldShowMarker(element)) return null
    return object : SparkConfigurationProducer {
      override fun createConfiguration() = createConfiguration(element)
      override fun isFromContext(configuration: ClusterSparkJobRunConfiguration) = isFromContext(element, configuration)
    }
  }

  override fun getSlowInfo(element: PsiElement): Info? {
    if (!shouldShowMarker(element)) return null
    val gutterPopupGroup = ActionManagerEx.getInstanceEx().getAction("BigDataTools.Deploy") as ActionGroup? ?: return null
    return Info(
      BigdatatoolsSparkSubmitIcons.SparkRun,
      { SparkMessagesBundle.message("spark.submit.gutter.icon.tooltip") },
      object : DumbAwareAction() {
        override fun actionPerformed(e: AnActionEvent) {
          val mouseEvent = e.inputEvent as? MouseEvent
          ActionGroupPopup(
            null, null, gutterPopupGroup, e.dataContext,
            ActionPlaces.POPUP, PresentationFactory(),
            ActionPopupOptions.create(false, false, true, false, -1, true, null), null).apply {
            when {
              mouseEvent != null -> show(RelativePoint(mouseEvent))
              else -> showInBestPositionFor(e.dataContext)
            }
          }
        }
      }
    )
  }
}