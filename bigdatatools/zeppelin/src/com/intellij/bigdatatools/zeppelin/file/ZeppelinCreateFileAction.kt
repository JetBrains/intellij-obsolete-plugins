// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.file

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.bigdatatools.common.constants.BdtPlugins

class ZeppelinCreateFileAction : CreateFileFromTemplateAction(), DumbAware {
  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    builder
      .setTitle(ZepMessagesBundle.message("action.create.zeppelin.file.title"))
      .addKind(ZepMessagesBundle.message("action.NewZeppelinFile.description"), ZeppelinIcons.ZEPPELIN_FILE,
               ZeppelinConstants.TEMPLATE_FILE_NAME)
  }


  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabledAndVisible)
      e.presentation.isEnabledAndVisible = BdtPlugins.isZeppelinPluginInstalled()
  }

  override fun getActionName(directory: PsiDirectory, newName: String,
                             templateName: String) = ZepMessagesBundle.message("action.create.zeppelin.notebook", newName)
}