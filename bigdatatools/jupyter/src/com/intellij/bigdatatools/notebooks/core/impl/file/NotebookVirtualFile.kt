// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.core.impl.file

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.notebook.editor.BackedVirtualFile
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil
import java.io.OutputStreamWriter

/**
 * Virtual file representing the notebook as a source code with cells.
 */
class NotebookVirtualFile(originalFile: VirtualFile,
                          val notebook: BasicNotebook,
                          requiredSaveOriginalFile: Boolean) : LightVirtualFile(originalFile,
                                                                                notebook.asSource(),
                                                                                originalFile.modificationStamp), BackedVirtualFile {
  private val parentFile: VirtualFile = originalFile

  init {
    if (requiredSaveOriginalFile)
      addSaveOriginalFileListener(originalFile)

    // This we need at least for showing breadcrumbs.
    RfsFileUtil.copyDriverIdAndPath(fromFile = originalFile, toFile = this)
  }

  fun setCustomFileType(fileType: LanguageFileType) = putCopyableUserData(FILE_TYPE_KEY, fileType)

  override fun getOriginFile() = parentFile
  override fun getOriginalFile() = parentFile

  override fun getParent() = if (originFile.parent?.isValid == true)
    originFile.parent
  else
    null

  override fun getUrl(): String = parentFile.url
  override fun getFileType() = getCopyableUserData(FILE_TYPE_KEY) ?: parentFile.fileType
  override fun toString(): String = "NotebookVirtualFile: $presentableUrl"

  private fun addSaveOriginalFileListener(originalFile: VirtualFile) {
    notebook.addNotebookChangeListener(object : NotebookChangeListener {
      override fun onEvent(notebookEvent: NotebookEvent) = invokeLater {
        runWriteAction {
          val document = FileDocumentManager.getInstance().getDocument(originalFile)!!
          if (document.isWritable) {
            document.setText(notebook.asJson())
          }
          else {
            // TODO: this is fallback to 'save on every file change' strategy, should be fixed as a part of large notebook support issue
            OutputStreamWriter(originalFile.getOutputStream(this@NotebookVirtualFile), Charsets.UTF_8)
              .use {
                it.write(notebook.asJson().toString())
              }
          }
        }
      }
    })
  }

  companion object {
    private val FILE_TYPE_KEY = Key<LanguageFileType>("NOTE_FILE_TYPE")
  }
}