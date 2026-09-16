package com.intellij.bigdatatools.emr.submit

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.ui.component.ScrollPaneUtils
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.jetbrains.spark.submit.run.common.ui.getFileSelectorValidationInfo
import software.amazon.awssdk.services.emr.model.HadoopJarStepConfig
import software.amazon.awssdk.services.emr.model.StepConfig
import java.awt.Dimension
import javax.swing.JComponent

class AddSparkStepDialog(private val project: Project, emrConnData: EmrDataManager, selectedId: String?) : DialogWrapper(project) {
  val configuration = EmrSparkSubmitConfigurationFactory.createTemplateConfiguration(project)
  val editor = EmrSparkSubmitConfigurationEditor(project, emrConnData, selectedId, configuration)

  init {
    title = EmrMessagesBundle.message("cluster.spark.step.add.action")
    Disposer.register(disposable, editor)
    init()
  }

  override fun createCenterPanel(): JComponent = ScrollPaneUtils.wrapWithScrollPane(editor.component).apply {
    preferredSize = Dimension(450, 450)
  }

  fun getResult(): StepConfig {
    val configuration = EmrSparkJobRunConfiguration(project, EmrSparkSubmitConfigurationFactory, "temp")
    editor.applyTo(configuration)
    val runProfileState = configuration.state

    val args = runProfileState.createCommandLineParams(isDebugMode = false)
    val hadoopJarStep = HadoopJarStepConfig.builder().jar("command-runner.jar").args(args).build()
    return StepConfig.builder().hadoopJarStep(hadoopJarStep)
      .name(configuration.name)
      .actionOnFailure(configuration.actionOnFailure)
      .build()
  }

  override fun doValidate() = editor.artifactPathField.getFileSelectorValidationInfo()
}