package com.intellij.dataWrangler.llm

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.Icon

interface DWCommandAction {
  val name: @Nls String
  val icon: Icon

  fun isAvailable(project: Project): Boolean

  // TODO remove AnActionEvent parameter after the AI Hub refactoring
  fun perform(actionEvent: AnActionEvent) {}

  companion object {
    val EP: ExtensionPointName<DWCommandAction> = ExtensionPointName.create("com.intellij.dataWrangler.llm.dwCommandAction")
  }
}