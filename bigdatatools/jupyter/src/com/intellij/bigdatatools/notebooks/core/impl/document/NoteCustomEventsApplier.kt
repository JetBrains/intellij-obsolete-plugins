package com.intellij.bigdatatools.notebooks.core.impl.document

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.NotNull

class NoteCustomEventsApplier {
  fun beforeChange(document: Document) {
    val textBefore = document.text
    document.putUserData(TEXT_BEFORE_CHANGE, textBefore)
  }

  fun afterChange(document: Document) {
    val noteDocumentListener = NoteDocumentFileUtil.getChangeListener(document) ?: return
    val oldText = document.getUserData(TEXT_BEFORE_CHANGE) ?: return
    document.putUserData(TEXT_BEFORE_CHANGE, null)

    if (oldText == document.text)
      return

    val preparedEvents = prepareRightDocumentChangesForNote(oldText, document, NotebookConstants.PARAGRAPH_DELIMITER.removeSuffix("\n"))
    applyRightDocumentChangesToNote(oldText, noteDocumentListener, preparedEvents)
  }

  private fun applyRightDocumentChangesToNote(oldText: String,
                                              noteDocumentListener: NoteDocumentChangeListener,
                                              preparedEvents: List<DocumentOp>) {
    val tempDocument = DocumentImpl(oldText, true)

    tempDocument.addDocumentListener(object : DocumentListener {
      override fun beforeDocumentChange(event: DocumentEvent) = noteDocumentListener.beforeDocumentChange(event)
      override fun documentChanged(event: DocumentEvent) = noteDocumentListener.documentChangedEx(event, false, false)
    })

    noteDocumentListener.startNoteModification()
    preparedEvents.forEach {
      tempDocument.replaceString(it.startOffset, it.oldLength + it.startOffset, it.newString)
    }
    noteDocumentListener.endNoteModification()
  }

  private fun prepareRightDocumentChangesForNote(oldText: String, document: Document, delimiter: String): List<DocumentOp> {
    val tempDocument = DocumentImpl(oldText, true)
    val events = mutableListOf<DocumentEvent>()
    tempDocument.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        events.add(event)
      }
    })
    tempDocument.replaceText(document.text, document.modificationStamp + 1)

    return events.flatMap { splitEvent(it, delimiter) ?: listOf(DocumentOp(it.offset, it.oldLength, it.newFragment)) }
  }

  private fun splitEvent(event: DocumentEvent, delimiter: String): List<DocumentOp>? {
    val prevChar = if (event.offset != 0)
      event.document.text.substring(event.offset - 1, endIndex = event.offset)
    else
      ""
    val oldFragment = event.oldFragment
    val newFragment = event.newFragment

    val oldOffsetDelimiter = findDelimiterOffset(oldFragment, prevChar, delimiter) ?: return null
    val newOffsetDelimiter = findDelimiterOffset(newFragment, prevChar, delimiter) ?: return null

    val remove1 = event.oldFragment.substring(0, oldOffsetDelimiter)
    val insert1 = event.newFragment.substring(0, newOffsetDelimiter)
    val remove2 = event.oldFragment.substring(oldOffsetDelimiter + 1)
    val insert2 = event.newFragment.substring(newOffsetDelimiter + 1)

    return listOf(
      DocumentOp(event.offset, remove1.length, insert1),
      DocumentOp(event.offset + insert1.length + 1, remove2.length, insert2)
    )
  }

  private fun findDelimiterOffset(text: CharSequence, prevSymbol: String, delimiter: String): Int? {
    if (text.startsWith(delimiter) && prevSymbol == "\n")
      return 0

    val index = text.indexOf("\n$delimiter") + 1

    return if (index > 0)
      index
    else
      null
  }

  private data class DocumentOp(val startOffset: Int, val oldLength: Int, val newString: @NotNull CharSequence)

  companion object {
    val TEXT_BEFORE_CHANGE = Key<String>("TEXT_BEFORE_BULK_UPDATE")
  }
}