package com.intellij.bigdatatools.notebooks.core.impl.document

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key

object NoteDocumentFileUtil {
  private val ignoreDocumentChange = Key<Boolean>("ignorePsiChange")

  fun setIgnoreDocumentChange(document: Document, ignore: Boolean) = document.putUserData(ignoreDocumentChange, ignore)

  fun getIgnoreDocumentChange(document: Document) = document.getUserData(ignoreDocumentChange) ?: false

  fun getChangeListener(document: Document) = document.getUserData(NoteDocumentChangeListener.KEY)
}