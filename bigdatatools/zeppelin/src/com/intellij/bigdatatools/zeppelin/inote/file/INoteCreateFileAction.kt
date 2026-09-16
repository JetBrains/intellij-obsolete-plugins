// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.inote.file

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

@Suppress("ComponentNotRegistered")
class INoteCreateFileAction : CreateFileFromTemplateAction(ZepMessagesBundle.message("file.interactive.notebook.name"),
                                                           ZepMessagesBundle.message("file.interactive.notebook.desc"),
                                                           INoteFileType.icon), DumbAware {
  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    builder
      .setTitle(ZepMessagesBundle.message("file.interactive.notebook.name"))
      .addKind(ZepMessagesBundle.message("file.interactive.notebook.desc"), INoteFileType.icon, INoteFileType.TEMPLATE_FILE_NAME)
  }


  override fun getActionName(directory: PsiDirectory, newName: String,
                             templateName: String) = ZepMessagesBundle.message("file.interactive.notebook.name")
}