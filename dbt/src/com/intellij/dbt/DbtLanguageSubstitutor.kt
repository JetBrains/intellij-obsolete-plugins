package com.intellij.dbt

import com.intellij.jinja.Jinja2Language
import com.intellij.lang.Language
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.LanguageSubstitutor
import com.intellij.sql.SqlFileType

class DbtLanguageSubstitutor : LanguageSubstitutor() {
  override fun getLanguage(file: VirtualFile, project: Project): Language? {
    if (file.extension == SqlFileType.DEFAULT_EXTENSION) {
      val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null
      if (DbtUtils.isDbtModule(module)) {
        val sqlDirs = DbtUtils.getDbtProjectSqlDirectories(module)
        if (sqlDirs.any { VfsUtilCore.isAncestor(it, file, true) }) {
          return Jinja2Language.INSTANCE
        }
      }
    }
    return null
  }
}