package com.intellij.bigdatatools.zeppelin.file

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

object ZeppelinFileType : LanguageFileType(ZeppelinLanguage) {
  override fun getName() = "Zeppelin"
  override fun getDescription() = ZepMessagesBundle.message("file.type.description")
  override fun getDefaultExtension() = "zpln"
  override fun getIcon(): Icon = ZeppelinIcons.ZEPPELIN_FILE
  override fun isReadOnly() = false
  override fun getCharset(file: VirtualFile, content: ByteArray): String = CharsetToolkit.UTF8

  val SHARED_NOTEBOOK_SELECTED_NAME = Key.create<String?>("REF_RESOLVE_RESULT")
}
