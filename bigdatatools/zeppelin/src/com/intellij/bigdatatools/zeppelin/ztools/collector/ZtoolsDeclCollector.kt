package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface ZtoolsDeclCollector {

  //null is returned when we can't really run the collector, for instance, in dumb mode
  fun collectDeclNames(project: Project, file: NotebookVirtualFile): Set<String>?

  fun supportsLanguage(language: Language): Boolean

  companion object {
    private const val ID: String = "com.intellij.bigdatatools.zeppelin.ztools.declscollector"

    private val EP_NAME: ExtensionPointName<ZtoolsDeclCollector> = ExtensionPointName.create(ID)

    fun allCollectors(): List<ZtoolsDeclCollector> = EP_NAME.extensionList
  }
}