package com.intellij.dbt

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiFile
import com.intellij.util.FileSearchUtil

private const val FILE_SEARCH_TIME_LIMIT = 1000L
private const val FILE_SEARCH_DEPTH_LIMIT = 5

class DbtDirectories {
  companion object {
    private fun getModelsDirectory(module: Module) = DbtUtils.getDbtDirectory(module)?.findDirectory("models")

    private fun getSeedsDirectory(module: Module) = DbtUtils.getDbtDirectory(module)?.findDirectory("seeds")

    private fun getModelFileName(modelName: String) = "$modelName.sql"

    private fun getSeedFileName(modelName: String) = "$modelName.csv"

    fun findModel(modelName: String, module: Module): PsiFile? {
      val modelsDirectory = getModelsDirectory(module) ?: return null
      val virtualFile = FileSearchUtil.findFileRecursively(modelsDirectory, getModelFileName(modelName), FILE_SEARCH_DEPTH_LIMIT, FILE_SEARCH_TIME_LIMIT).findFirst()
      return virtualFile?.findPsiFile(module.project)
    }

    fun findSeedFile(seedName: String, module: Module): PsiFile? {
      val seedsDirectory = getSeedsDirectory(module) ?: return null
      val virtualFile = FileSearchUtil.findFileRecursively(seedsDirectory, getSeedFileName(seedName), FILE_SEARCH_DEPTH_LIMIT, FILE_SEARCH_TIME_LIMIT).findFirst()
      return virtualFile?.findPsiFile(module.project)
    }
  }
}