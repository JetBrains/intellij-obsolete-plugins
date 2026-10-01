package com.intellij.dbt.projectView

import com.intellij.dbt.DbtIcons
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.DbtUtils.Companion.getDbtSettings
import com.intellij.ide.IconProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import javax.swing.Icon

class DbtIconProvider : IconProvider() {
  override fun getIcon(element: PsiElement, flags: Int): Icon? {
    if (element is PsiDirectory) {
      if (DbtUtils.containsDbtProjectFile(element.virtualFile)) {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val dbtSettings = getDbtSettings(module) ?: return null
        if (element.virtualFile.path == dbtSettings.dbtProjectPath?.virtualFile?.path) {
          return DbtIcons.Dbt
        }
      }
    }
    return null
  }
}