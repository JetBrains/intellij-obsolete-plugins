package com.intellij.bigdatatools.plugin.spark.python.submit.wizard

import com.intellij.openapi.application.EDT
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.wizard.SparkProjectType
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.newProjectWizard.PyV3ProjectTypeSpecificSettings
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.installPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class PyV3SparkSettings(
  var projectType: SparkProjectType = SparkProjectType.BATCH,
) : PyV3ProjectTypeSpecificSettings {
  override suspend fun generateProject(module: Module, baseDir: VirtualFile, sdk: Sdk): PyResult<Unit> = coroutineScope {
    withContext(Dispatchers.EDT) {
      PySparkProjectGenerator.Utils.setupProject(module.project, baseDir, sdk, projectType)
    }
    PythonPackageManager.forSdk(module.project, sdk).installPackages("pyspark").mapSuccess { }
  }
}