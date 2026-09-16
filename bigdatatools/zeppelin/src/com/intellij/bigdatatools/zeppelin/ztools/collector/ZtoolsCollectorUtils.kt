package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.lang.Language
import com.intellij.openapi.project.Project

internal object ZtoolsCollectorUtils {
  fun collectTableNames(project: Project, file: NotebookVirtualFile) = ZtoolsWordsCollector.allCollectors().flatMap { collector ->
    collector.collectTableNames(project, file).map { it.copy(database = it.database.lowercase(), table = it.table.lowercase()) }.toSet()
  }

  fun collectDeclNames(primaryLanguage: String, project: Project, file: NotebookVirtualFile): List<String> {
    val language = Language.getRegisteredLanguages().find { it.id.lowercase() == primaryLanguage.lowercase() } ?: return emptyList()
    val collectors = ZtoolsDeclCollector.allCollectors().filter { it.supportsLanguage(language) }

    return if (collectors.isEmpty())
      emptyList()
    else
      collectors.flatMap { it.collectDeclNames(project, file) ?: emptySet() }
  }

}