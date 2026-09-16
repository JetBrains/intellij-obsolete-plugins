package com.jetbrains.bigdatatools.dataproc.ui.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.run.common.ui.SparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.run.ui.CommonSparkEditorUtil
import java.util.function.Supplier
import javax.swing.JTextField

class DataprocMainClassField(val project: Project,
                             val disposable: Disposable,
                             val dataManager: DataprocDataManager,
                             val pathProvider: () -> FilePath?,
                             val clusterNameProvider: () -> String?) : ComponentWithBrowseButton<JTextField>(
  JTextField(SparkSubmitConfigurationEditor.TEXT_FIELD_COLUMNS), null) {

  private var foundClasses = listOf<String>()

  init {
    addActionListener {
      val validator = ComponentValidator.getInstance(this).orElse(null)

      if (validator != null) {
        validator.revalidate()
        if (validator.validationInfo == null) {
          refreshFoundClasses()
          onChooseClassNameField()
        }
      }
      else {
        refreshFoundClasses()
        onChooseClassNameField()
      }
    }

    val supplier = Supplier {
      val filePath = pathProvider()
      if (filePath?.path?.isBlank() != false) {
        ValidationInfo(DataprocMessagesBundle.message("settings.application.class.name.error.msg"), this.childComponent)
      }
      else {
        null
      }
    }

    ComponentValidator(disposable)
      .withValidator(supplier)
      .withFocusValidator(supplier)
      .andRegisterOnDocumentListener(this.childComponent)
      .installOn(this)
  }

  private fun refreshFoundClasses() {
    foundClasses = run {
      val filePath = pathProvider() ?: return@run emptyList()
      return@run CommonSparkEditorUtil.getFoundClasses(project, filePath) { resolveArtifact(it, filePath) }
    }
  }

  private fun resolveArtifact(indicator: ProgressIndicator, filePath: FilePath?): String? {
    val artifactPath = filePath ?: return null
    val clusterName = clusterNameProvider() ?: return null
    val sourceRfsPath = dataManager.dependsManager.downloadFileToTemp(project, indicator, artifactPath, clusterName) ?: return null
    return sourceRfsPath.absolutePath
  }

  private fun onChooseClassNameField() {
    val selected = childComponent.text
    val newName = CommonSparkEditorUtil.selectClassName(project, foundClasses, selected) ?: return
    childComponent.text = newName
  }
}