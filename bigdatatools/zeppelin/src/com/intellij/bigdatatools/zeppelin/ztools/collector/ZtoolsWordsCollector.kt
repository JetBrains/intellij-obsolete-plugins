package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface ZtoolsWordsCollector {
  fun collectTableNames(project: Project, file: NotebookVirtualFile): Set<ZtoolsRefSqlTableInfo> = emptySet()

  fun supportsLanguage(language: Language): Boolean

  companion object {
    private const val ID: String = "com.intellij.bigdatatools.zeppelin.ztools.wordscollector"

    private val EP_NAME: ExtensionPointName<ZtoolsWordsCollector> = ExtensionPointName.create(ID)

    private fun filterWord(aString: String): Boolean =
      aString.isNotEmpty() && (aString.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '@' || it == '#' || it == '$' || it == '!'} || aString.startsWith("`") && aString.endsWith("`"))

    fun getWords(aString: String) = aString.split(" ").filter(Companion::filterWord).map { it.replace("\\", "\\\\") }

    fun allCollectors(): List<ZtoolsWordsCollector> = EP_NAME.extensionList

    //when for any of these languages there is a cell with the language and no words collector for language, default words collection operation to null
    //(i.e. perform no sql tables filtration at all since we can't properly guarantee that we traverse required cells)
    val requiredLanguagesSupportIds: List<String> = listOf("Scala", "Python", "SparkSQL")
  }
}