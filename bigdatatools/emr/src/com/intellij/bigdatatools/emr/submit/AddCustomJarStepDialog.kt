package com.intellij.bigdatatools.emr.submit

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.getValidationInfo
import com.intellij.bigdatatools.coreUi.settings.withNonEmptyValidator
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.ui.component.EmrFileSelector
import com.intellij.bigdatatools.emr.ui.component.EmrTypesSerializer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.spark.submit.run.common.ui.getFileSelectorValidationInfo
import com.jetbrains.spark.submit.run.common.ui.withNonEmptyValidator
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.Nls
import software.amazon.awssdk.services.emr.model.ActionOnFailure
import software.amazon.awssdk.services.emr.model.HadoopJarStepConfig
import software.amazon.awssdk.services.emr.model.StepConfig
import java.awt.Dimension
import javax.swing.JComponent

class AddCustomJarStepDialog(project: Project,
                             dataManager: EmrDataManager,
                             clusterId: String?,
                             @NlsContexts.DialogTitle title: String,
                             @Nls defaultName: String) : EmrAddStepDialog(project, dataManager, clusterId, title, defaultName) {

  private val jarField = EmrFileSelector(SparkMessagesBundle.message("dialog.artifactPath.title"),
                                         project, driversProvider, withS3 = true).withNonEmptyValidator(disposable)

  init {
    nameField.withNonEmptyValidator(disposable)
    Disposer.register(disposable, driversProvider)
    init()
  }

  override fun createCenterPanel(): JComponent = MigPanel().apply {
    row(HdfsMessagesBundle.message("emr.spark.submit.editor.name"), nameField)
    row(HdfsMessagesBundle.message("emr.spark.submit.editor.jar.loc"), jarField.component)
    row(HdfsMessagesBundle.message("emr.spark.submit.editor.args"), argumentsField)
    minimumSize = Dimension(400, minimumSize.height)
  }

  override fun getResult(): StepConfig {
    val args = ParametersListUtil.parse(argumentsField.text, false)
    val path = convertPath(jarField.path)
    val hadoopJarStep = HadoopJarStepConfig.builder().jar(path).args(args).build()
    return StepConfig.builder()
      .hadoopJarStep(hadoopJarStep)
      .name(nameField.text)
      .actionOnFailure(actionOnFailureField.item)
      .build()
  }

  fun initByConfig(stepConfig: StepConfig) {
    jarField.path = EmrTypesSerializer.fromText(stepConfig.hadoopJarStep().jar())
    argumentsField.text = ParametersListUtil.join(stepConfig.hadoopJarStep().args())
    actionOnFailureField.item = ActionOnFailure.knownValues().firstOrNull { it.name == stepConfig.actionOnFailureAsString() }
                                ?: ActionOnFailure.CONTINUE
    nameField.text = stepConfig.name()
    repaint()
  }

  override fun doValidate() = nameField.getValidationInfo() ?: jarField.getFileSelectorValidationInfo()

  override fun getDimensionServiceKey() = "bigdatatools.aws.emr.jar.step.dialog.bounds"
}