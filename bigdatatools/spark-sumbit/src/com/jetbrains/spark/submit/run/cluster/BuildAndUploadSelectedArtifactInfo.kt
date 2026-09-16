package com.jetbrains.spark.submit.run.cluster

import com.intellij.execution.BeforeRunTask
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.ssh.upload.UploadBeforeRunTask
import com.jetbrains.spark.submit.run.ssh.upload.UploadBeforeRunTaskProvider

class DefaultSelectedArtifactInfo(override val filePath: FilePath) : SelectedArtifactInfo {
  override fun createBeforeTasks(existingTasks: List<BeforeRunTask<*>>): List<BeforeRunTask<*>> = existingTasks
}

class UploadExistingSelectedArtifactInfo(private val artifactPath: String) : SelectedArtifactInfo {
  override val filePath: FilePath get() = FilePath(FileType.UPLOAD, artifactPath)
  override val allowMissing: Boolean get() = false

  override fun createBeforeTasks(existingTasks: List<BeforeRunTask<*>>): List<BeforeRunTask<*>> {
    val result = existingTasks.toMutableList()
    if (!result.any { task -> task.providerId == UploadBeforeRunTaskProvider.ID }) {
      result.add(UploadBeforeRunTask())
    }
    return result
  }
}

abstract class BuildAndUploadSelectedArtifactInfo(override val filePath: FilePath) : SelectedArtifactInfo {

  abstract fun isMyBuildTask(task: BeforeRunTask<*>): Boolean
  abstract fun createBuildTask(): BeforeRunTask<*>?
  override val allowMissing: Boolean get() = true

  override fun createBeforeTasks(existingTasks: List<BeforeRunTask<*>>) : List<BeforeRunTask<*>> {
    val result = existingTasks.toMutableList()
    val indexOfGradle = result.indexOfFirst(::isMyBuildTask)
    val indexOfUpload = result.indexOfFirst { task -> task.providerId == UploadBeforeRunTaskProvider.ID }
    when {
      indexOfUpload < 0 && indexOfGradle < 0 -> {
        createBuildTask()?.let { result.add(it) }
        result.add(UploadBeforeRunTask())
      }
      indexOfGradle < 0 -> {
        createBuildTask()?.let { result.add(indexOfUpload, it) }
      }
      indexOfUpload < 0 -> {
        result.add(indexOfGradle + 1, UploadBeforeRunTask())
      }
      indexOfGradle > indexOfUpload -> {
        result.add(indexOfGradle + 1, result[indexOfUpload])
        result.removeAt(indexOfUpload)
      }
    }
    return result
  }
}

