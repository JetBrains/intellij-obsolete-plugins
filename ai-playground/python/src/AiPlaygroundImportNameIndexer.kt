package com.intellij.aiplayground.python

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.VoidDataExternalizer
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.psi.PyFromImportStatement
import com.jetbrains.python.psi.PyImportStatement

object AiPlaygroundImportNameKey {
  val NAME: ID<String, Void> = ID.create("ai.playground.py.import.libraries")
}

class AiPlaygroundImportNameIndexer : FileBasedIndexExtension<String, Void>() {
  val AI_PLAYGROUND_LIBRARIES_WHITELIST: List<String> = listOf(
    "openai",
    "langchain",
    "langgraph",
    "anthropic",
    "mistralai",
    "deepseek",
    "google.genai",
    "openrouter",
    "ollama",
    "pydantic_ai",
    "crewai"
  )

  override fun getName(): ID<String, Void> = AiPlaygroundImportNameKey.NAME

  override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { inputData ->
    val result = mutableMapOf<String, Void?>()
    if (inputData.fileType !is PythonFileType) {
      return@DataIndexer result
    }
    val module = ModuleUtilCore.findModuleForFile(inputData.psiFile)
    if (module == null) {
      return@DataIndexer result
    }

    inputData.psiFile.children.filter {it is PyImportStatement || it is PyFromImportStatement}.forEach {
      val importedPackages = mutableListOf<String>()
      if (it is PyImportStatement) {
        it.importElements.mapNotNull {importElement -> importElement.name}.forEach { importedPackage ->
          importedPackages.add(importedPackage)
        }
      } else if (it is PyFromImportStatement) {
        it.importSource?.asQualifiedName()?.toString()?.let { importedPackage ->
          importedPackages.add(importedPackage)
        }
      }

      importedPackages.forEach {importedPackage ->
        for (lib in AI_PLAYGROUND_LIBRARIES_WHITELIST) {
          if (importedPackage.startsWith(lib)) {
            result[importedPackage] = null
            break
          }
        }
      }
    }

    result
  }

  override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(PythonFileType.INSTANCE)

  override fun dependsOnFileContent(): Boolean = true

  override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor()

  override fun getVersion(): Int = 3

  override fun getValueExternalizer(): DataExternalizer<Void> = VoidDataExternalizer.INSTANCE
}