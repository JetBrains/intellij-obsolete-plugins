package com.jetbrains.spark.submit.run.ssh.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.run.common.ui.TextFieldFileSelector

// TODO inline
fun SshFileSelector(
  @DialogTitle title: String,
  project: Project,
  fileSelectorType: FileSelectorType,
  filePathSerializer: FilePathSerializer,
  defaultText: String = "",
  sshConfig: SshConfigProvider
) = TextFieldFileSelector(fileSelectorType, filePathSerializer, SshFileSelectorContextImpl(project, title, sshConfig), defaultText)

