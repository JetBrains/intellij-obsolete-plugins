package com.intellij.bigdatatools.plugin.spark.submit.gutter

import com.intellij.execution.Location
import com.intellij.execution.RunManager
import com.intellij.execution.impl.EditConfigurationsDialog
import com.intellij.execution.impl.ProjectRunConfigurationConfigurable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import javax.swing.tree.DefaultMutableTreeNode

class SubmitCreateAction : AnAction() {

  var location: Location<*>? = null

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val location = e.getData(Location.DATA_KEY) ?: this.location ?: return
    val sparkConfigurationProducer = SparkConfigurationProducerProvider.findSparkConfigurationProducer(location) ?: return
    val configurationSettings = sparkConfigurationProducer.createConfiguration()
    EditConfigurationsDialog(project, object : ProjectRunConfigurationConfigurable(project) {
      override fun getInitialSelectedConfiguration() = configurationSettings
      override fun addRunConfigurationsToModel(model: DefaultMutableTreeNode) {
        try {
          RunManager.getInstance(project).addConfiguration(configurationSettings)
          super.addRunConfigurationsToModel(model)
        }
        finally {
          RunManager.getInstance(project).removeConfiguration(configurationSettings)
        }
      }
    }, e.dataContext).showAndGet()
  }

  override fun update(e: AnActionEvent) {
    val location = e.getData(Location.DATA_KEY)
    if (location != null) {
      this.location = location
    }
    e.presentation.isEnabledAndVisible = e.project != null && e.getData(LangDataKeys.PSI_FILE) != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}