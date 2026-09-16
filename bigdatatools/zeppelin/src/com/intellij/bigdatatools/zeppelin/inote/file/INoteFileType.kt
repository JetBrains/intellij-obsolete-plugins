package com.intellij.bigdatatools.zeppelin.inote.file

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

object INoteFileType : LanguageFileType(ZeppelinLanguage) {
  override fun getName() = "INotebook"
  override fun getDescription() = ZepMessagesBundle.message("file.interactive.notebook.desc")
  override fun getDefaultExtension() = "inote"
  override fun getIcon(): Icon = ZeppelinIcons.INTERACTIVE_NOTE_ICON
  override fun isReadOnly() = false
  override fun getCharset(file: VirtualFile, content: ByteArray): String = CharsetToolkit.UTF8
  override fun getDisplayName() = ZepMessagesBundle.message("file.interactive.notebook.desc")

  const val TEMPLATE_FILE_NAME = "Interactive Notebook"
}