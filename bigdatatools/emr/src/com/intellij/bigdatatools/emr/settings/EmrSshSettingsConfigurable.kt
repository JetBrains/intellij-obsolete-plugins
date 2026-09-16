package com.intellij.bigdatatools.emr.settings

import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.NonNls
import java.awt.Component

object EmrSshSettingsConfigurable {
  fun getOrAskUserForCredentialFile(project: Project?,
                                    keyName: String,
                                    parentComponent: Component?): @NonNls String? = invokeAndWaitIfNeeded {
    val storedPath = EmrSshKeysStorage.getInstance().getPathForKey(keyName)
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

    val path = choosePathToKey(keyName, project, parentComponent, null) ?: return@invokeAndWaitIfNeeded null

    EmrSshKeysStorage.getInstance().put(keyName, path)
    return@invokeAndWaitIfNeeded path
  }

  private fun choosePathToKey(keyName: String,
                              project: Project?,
                              parentComponent: Component?, prevSelected: VirtualFile?): String? {
    val chooserDescription = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
      .withDescription(HdfsMessagesBundle.message("emr.label.choose.key.file.for.aws.pair", keyName))
      .withTitle(HdfsMessagesBundle.message("emr.dialog.title.select.key.ssh.file"))


    val file = FileChooserFactory.getInstance()
      .createFileChooser(chooserDescription, project, parentComponent)
      .choose(project, *listOfNotNull(prevSelected).toTypedArray()).firstOrNull()
    if (file == null)
      return null

    if (file.length > 1024 * 1024) {
      Messages.showErrorDialog(project, EmrMessagesBundle.message("choose.pem.error.incorrect.msg"),
                               EmrMessagesBundle.message("choose.pem.error.incorrect.title"))
      return null
    }

    return file.path
  }
}