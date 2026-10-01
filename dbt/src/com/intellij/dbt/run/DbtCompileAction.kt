package com.intellij.dbt.run

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtIcons
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.console.DbtConsoleService
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.console.getDbtCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAwareAction

class DbtCompileAction : DumbAwareAction() {
  private val service = service<DbtConsoleService>()

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return
    val dbtDirectory = DbtUtils.getDbtDirectory(module) ?: return

    val cmd = getDbtCommandLine(DbtCommand.COMPILE, module, dbtDirectory.path)

    val selectedText = editor.selectionModel.selectedText
    if (!selectedText.isNullOrEmpty()) {
      cmd.withParameters("--inline", selectedText)
    } else {
      cmd.withParameters("--select", file.virtualFile.nameWithoutExtension)
    }

    val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(cmd)
    ProcessTerminatedListener.attach(processHandler)

    service.showConsoleViewContent(module.project, processHandler)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    e.presentation.icon = DbtIcons.Dbt
    val file = e.getData(CommonDataKeys.PSI_FILE) ?: return

    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    if (editor.selectionModel.selectedText.isNullOrEmpty()) {
      e.presentation.text = DbtBundle.message("action.dbtCompileAction.text")
      e.presentation.description = DbtBundle.message("action.dbtCompileAction.description")
    } else {
      e.presentation.text = DbtBundle.message("action.dbtCompileSelectedFragmentAction.text")
      e.presentation.description = DbtBundle.message("action.dbtCompileSelectedFragmentAction.description")
    }


    if (!DbtUtils.isSqlDialect(file)) {
      return
    }
    e.presentation.isEnabledAndVisible = true
  }
}