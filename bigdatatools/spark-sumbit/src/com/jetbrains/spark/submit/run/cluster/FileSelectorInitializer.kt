package com.jetbrains.spark.submit.run.cluster

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.spark.submit.model.SelectedArtifactInfo

interface FileSelectorInitializer {

  fun guessFromContext(project: Project, file: PsiFile): SelectedArtifactInfo?

  companion object {
    private val EP_NAME = ExtensionPointName.create<FileSelectorInitializer>("com.intellij.bigdatatools.file.selector.initializer")
    fun getAll() = EP_NAME.extensionList
  }

}