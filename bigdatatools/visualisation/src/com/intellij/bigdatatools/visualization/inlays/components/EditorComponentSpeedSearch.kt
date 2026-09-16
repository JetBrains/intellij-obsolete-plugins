package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.impl.livePreview.LivePreviewController
import com.intellij.find.impl.livePreview.SearchResults
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

// First try to create quick search for Editor. This functionality is not ready.
open class EditorComponentSpeedSearch protected constructor(editor: EditorImpl) : SimpleSpeedSearch<JComponent>(editor.contentComponent) {

  private val disposable = Disposer.newDisposable()

  private val findModel = createDefaultFindModel(editor.project!!)
  private val searchResult = SearchResults(editor, editor.project!!)
  private val livePreviewController = LivePreviewController(searchResult, null, disposable)

  private fun createDefaultFindModel(project: Project) = FindModel().apply {
    copyFrom(FindManager.getInstance(project).findInFileModel)
    isPromptOnReplace = false
  }

  override fun findNextElement(s: String): Any? {
    searchResult.nextOccurrence(true)
    return s
  }

  override fun findPreviousElement(s: String): Any? {
    searchResult.prevOccurrence(true)
    return s
  }

  override fun findElement(s: String): Any? {
    findModel.stringToFind = s
    searchResult.nextOccurrence(true)
    return s
  }

  override fun findFirstElement(s: String): Any? {
    findModel.stringToFind = s
    return s
  }

  override fun findLastElement(s: String): Any? {
    searchResult.nextOccurrence(true)
    return s
  }

  companion object {
    fun installOn(editor: EditorImpl) = EditorComponentSpeedSearch(editor)
  }
}