package com.intellij.bigdatatools.plugin.spark.python.submit.wizard

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.wizard.SparkProjectType
import com.jetbrains.python.sdk.PythonSdkUpdater

class PySparkProjectGenerator {

  internal object Utils {
    @RequiresEdt
    fun setupProject(project: Project, baseDir: VirtualFile, sdk: Sdk, projectType: SparkProjectType) {
      WriteAction.run<Throwable> {
        val directory = PsiManager.getInstance(project).findDirectory(baseDir)!!
        val templateName = when (projectType) {
          SparkProjectType.BATCH -> "pyspark_batch"
          SparkProjectType.STREAMING -> "pyspark_stream"
        }
        val mainTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(templateName)
        val mainFile = FileTemplateUtil.createFromTemplate(mainTemplate, "main.py", null, directory) as PsiFile

        invokeLater {
          FileEditorManager.getInstance(project).apply {
            openFile(mainFile.virtualFile, true)
          }
        }
      }

      PythonSdkUpdater.scheduleUpdate(sdk, project)
    }

  }
}