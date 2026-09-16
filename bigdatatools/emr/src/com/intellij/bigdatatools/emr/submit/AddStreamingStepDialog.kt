package com.intellij.bigdatatools.emr.submit

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.getValidationInfo
import com.intellij.bigdatatools.coreUi.settings.withNonEmptyValidator
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.ui.component.EmrFileSelector
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.jetbrains.spark.submit.run.common.ui.getFileSelectorValidationInfo
import com.jetbrains.spark.submit.run.common.ui.withNonEmptyValidator
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.Nls
import software.amazon.awssdk.services.emr.model.HadoopJarStepConfig
import software.amazon.awssdk.services.emr.model.StepConfig
import java.awt.Dimension
import javax.swing.JComponent

class AddStreamingStepDialog(project: Project,
                             dataManager: EmrDataManager,
                             clusterId: String?,
                             @NlsContexts.DialogTitle title: String,
                             @Nls defaultName: String) : EmrAddStepDialog(project, dataManager, clusterId, title, defaultName) {

  private val mapperField = EmrFileSelector(HdfsMessagesBundle.message("emr.step.mapper.choose"),
                                            project,
                                            driversProvider,
                                            withS3 = true).withNonEmptyValidator(disposable)
  private val reducerField = EmrFileSelector(HdfsMessagesBundle.message("emr.step.reducer.choose"),
                                             project,
                                             driversProvider,
                                             withS3 = true).withNonEmptyValidator(disposable)
  private val inputS3LocationField = EmrFileSelector(HdfsMessagesBundle.message("emr.step.s3.input.choose"),
                                                     project,
                                                     driversProvider,
                                                     withS3 = true).withNonEmptyValidator(disposable)
  private val outputS3LocationField = EmrFileSelector(HdfsMessagesBundle.message("emr.step.s3.output.choose"),
                                                      project,
                                                      driversProvider,
                                                      withS3 = true).withNonEmptyValidator(disposable)

  init {
    nameField.withNonEmptyValidator(disposable)
    Disposer.register(disposable, driversProvider)
    init()
  }

  override fun createCenterPanel(): JComponent = MigPanel().apply {
    row(HdfsMessagesBundle.message("emr.spark.submit.editor.name"), nameField)
    row(EmrMessagesBundle.message("emr.spark.submit.editor.mapper"), mapperField.component)
    row(EmrMessagesBundle.message("emr.spark.submit.editor.reducer"), reducerField.component)
    row(EmrMessagesBundle.message("emr.spark.submit.editor.inputs3"), inputS3LocationField.component)
    row(EmrMessagesBundle.message("emr.spark.submit.editor.outputs3"), outputS3LocationField.component)
    row(HdfsMessagesBundle.message("emr.spark.submit.editor.args"), argumentsField)
    minimumSize = Dimension(400, minimumSize.height)
  }

  override fun getResult(): StepConfig {
    val args = ParametersListUtil.parse(argumentsField.text, false)
    val mapperPath = convertPath(mapperField.path)
    val reducerPath = convertPath(reducerField.path)
    val inputPath = convertPath(inputS3LocationField.path).withSlash()
    val outputPath = convertPath(outputS3LocationField.path).withSlash()

    val preparedArgs = listOf("hadoop-streaming",
                              "-files",
                              "$mapperPath,$reducerPath",
                              "-mapper",
                              mapperPath.substringAfterLast("/"),
                              "-reducer",
                              reducerPath.substringAfterLast("/"),
                              "-input",
                              inputPath,
                              "-output",
                              outputPath
    )
    val hadoopJarStep = HadoopJarStepConfig.builder().jar("command-runner.jar").args(preparedArgs + args).build()
    return StepConfig.builder().hadoopJarStep(hadoopJarStep).name(nameField.text).actionOnFailure(actionOnFailureField.item).build()
  }

  override fun doValidate() = nameField.getValidationInfo() ?: mapperField.getFileSelectorValidationInfo()
                              ?: reducerField.getFileSelectorValidationInfo() ?: inputS3LocationField.getFileSelectorValidationInfo()
                              ?: outputS3LocationField.getFileSelectorValidationInfo()

  override fun getDimensionServiceKey() = "bigdatatools.aws.emr.streaming.step.dialog.bounds"
}