package com.intellij.bigdatatools.notebooks.style

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "BdtNoteStyleSettings", storages = [Storage("BdtNoteStyleSettings.xml")])
class NoteStyleSettings : PersistentStateComponent<NoteStyleSettings> {
  var needConfirmCellDelete = true
  var needConfirmCellSplit = true
  var needConfirmCellMerge = true
  var linesNumbering = LinesNumberingMode.CELL
  var editorSoftWraps = true

  override fun getState() = this

  override fun loadState(state: NoteStyleSettings) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    fun getInstance(): NoteStyleSettings = service()
  }
}