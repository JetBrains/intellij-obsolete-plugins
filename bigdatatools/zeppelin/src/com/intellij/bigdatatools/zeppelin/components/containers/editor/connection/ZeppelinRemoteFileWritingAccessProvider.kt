// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.WritingAccessProvider

class ZeppelinRemoteFileWritingAccessProvider : WritingAccessProvider() {
  override fun requestWriting(files: MutableCollection<out VirtualFile>): MutableCollection<VirtualFile> =
    files.asSequence().filterIsInstance<ZeppelinRemoteFile>().filter { !it.isWritable && NotebookFileUtil.isRemote(it) }.toMutableList()

  override fun getReadOnlyMessage(): String = ZepMessagesBundle.message("connection.notebook.disconnected.read.only")
}