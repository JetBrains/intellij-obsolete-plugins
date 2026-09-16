package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.NonNls
import java.awt.Component

object DataprocSshSettingsConfigurable {
  fun getOrAskUserForCredentialFile(project: Project?,
                                    keyName: String,
                                    parentComponent: Component?): @NonNls String? = invokeAndWaitIfNeeded {
    val storedPath = DataprocSshKeysStorage.getInstance().getPathForKey(keyName)
    if (storedPath != null)
      return@invokeAndWaitIfNeeded storedPath

    val res = Messages.showOkCancelDialog(project,
                                          HdfsMessagesBundle.message("emr.dialog.title.select.key.info.msg", keyName),
                                          HdfsMessagesBundle.message("emr.dialog.title.select.key.info.title"),
                                          HdfsMessagesBundle.message("emr.dialog.title.select.key.info.ok"),
                                          HdfsMessagesBundle.message("emr.dialog.title.select.key.info.cancel"),
                                          Messages.getInformationIcon())

    if (res != Messages.OK)
      return@invokeAndWaitIfNeeded null

    val chooserDescription = FileChooserDescriptorFactory.createSingleFileDescriptor()
      .withDescription(HdfsMessagesBundle.message("emr.label.choose.key.file.for.aws.pair", keyName))
      .withTitle(HdfsMessagesBundle.message("emr.dialog.title.select.key.ssh.file"))
    val files = FileChooserFactory.getInstance().createFileChooser(chooserDescription, project, parentComponent).choose(project)
    val path = files.firstOrNull()?.path
    if (path != null) {
      DataprocSshKeysStorage.getInstance().put(keyName, path)
    }
    return@invokeAndWaitIfNeeded path
  }
}