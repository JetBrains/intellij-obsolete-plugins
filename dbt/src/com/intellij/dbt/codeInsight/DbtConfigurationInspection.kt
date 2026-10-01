package com.intellij.dbt.codeInsight

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtUtils.Companion.getDbtSettings
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.sql.psi.SqlFile

class DbtConfigurationInspection : LocalInspectionTool() {
  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    if (!isOnTheFly || file !is SqlFile) return ProblemDescriptor.EMPTY_ARRAY

    val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return ProblemDescriptor.EMPTY_ARRAY
    val dbtSettings = getDbtSettings(module) ?: return ProblemDescriptor.EMPTY_ARRAY
    if (!dbtSettings.reviewed) {
      val descriptor = manager.createProblemDescriptor(file, DbtBundle.message("dbt.configuration.inspection.name"), true,
                                                       arrayOf(MyQuickFix()), ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
      return arrayOf(descriptor)
    }
    return ProblemDescriptor.EMPTY_ARRAY
  }
}

private class MyQuickFix : IntentionAndQuickFixAction() {
  override fun getName() = familyName

  override fun getFamilyName() = DbtBundle.message("quickfix.configure.dbt")

  override fun applyFix(project: Project, psiFile: PsiFile, editor: Editor?) {
    ShowSettingsUtilImpl.showSettingsDialog(project, "com.intellij.dbt.settings.DbtProjectConfigurable", "")
  }

  override fun startInWriteAction() = false
}