package com.jetbrains.spark.submit.run.ssh.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.run.common.ui.FileMultiSelector
import com.jetbrains.spark.submit.run.common.ui.FileSelectorContextImpl

// TODO inline
fun SshFileMultiSelector(
  project: Project,
  fileSelectorType: FileSelectorType,
  filePathSerializer: FilePathSerializer,
  @Label label: String,
  @DialogTitle dialogTitle: String,
  sshConfig: SshConfigProvider
) = FileMultiSelector(fileSelectorType,
                      filePathSerializer,
                      label,
                      SshFileSelectorContextImpl(project, dialogTitle, sshConfig))

class SshFileSelectorContextImpl(
  project: Project,
  @DialogTitle dialogTitle: String,
  sshConfig: SshConfigProvider
) : FileSelectorContextImpl(project, dialogTitle), SshFileSelectorContext, SshConfigProvider by sshConfig