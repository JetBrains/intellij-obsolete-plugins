package com.intellij.bigdatatools.emr.submit

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.dependend.drivers.EmrDriversProviderImpl
import com.intellij.bigdatatools.emr.ui.component.EmrComponentsCreator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.JBTextField
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import org.jetbrains.annotations.Nls
import software.amazon.awssdk.services.emr.model.StepConfig

abstract class EmrAddStepDialog(project: Project,
                                dataManager: EmrDataManager,
                                clusterId: String?,
                                @NlsContexts.DialogTitle title: String,
                                @Nls defaultName: String) : DialogWrapper(project) {
  protected val driversProvider = EmrDriversProviderImpl(project, dataManager, clusterId)

  protected val nameField = JBTextField(defaultName)
  protected val argumentsField = RawCommandLineEditor()
  protected val actionOnFailureField = EmrComponentsCreator.createActionOnFailureField()

  init {
    this.title = title
  }

  override fun init() {
    Disposer.register(disposable, driversProvider)
    super.init()
  }

  abstract fun getResult(): StepConfig

  protected fun convertPath(filePath: FilePath) = if (filePath.type == FileType.S3)
    filePath.toString()
  else
    filePath.path
}