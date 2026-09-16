package com.jetbrains.spark.submit.model

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper
import com.intellij.ide.DataManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.spark.submit.run.cluster.DefaultSelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.UploadExistingSelectedArtifactInfo
import javax.swing.JComponent

interface SelectedArtifactInfo {
  val filePath: FilePath
  val allowMissing: Boolean get() = true
  fun createBeforeTasks(existingTasks: List<BeforeRunTask<*>>) : List<BeforeRunTask<*>>
}

fun FilePath.inferSelectedArtifactInfo(): SelectedArtifactInfo {
  return when (this.type) {
    FileType.UPLOAD -> UploadExistingSelectedArtifactInfo(this.path)
    else -> DefaultSelectedArtifactInfo(this)
  }
}

@RequiresEdt
fun initBeforeTask(selectedArtifactInfo: SelectedArtifactInfo, componentInConfigurationEditor: JComponent) {
  val dataContext = DataManager.getInstance().getDataContext(componentInConfigurationEditor)
  val editorWrapper = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext)
  checkNotNull(editorWrapper)
  val buildArtifactsTasks: List<BeforeRunTask<*>> = editorWrapper.stepsBeforeLaunch
  val tasksToAdd = selectedArtifactInfo.createBeforeTasks(buildArtifactsTasks)
  editorWrapper.replaceBeforeLaunchSteps(tasksToAdd)
}