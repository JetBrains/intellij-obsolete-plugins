package com.intellij.bigdatatools.notebooks.core.api.nbformat

import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.BasicNotebookImpl
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookMetadataAware
import com.intellij.openapi.util.TextRange
import java.util.Date

interface NotebookCell : NotebookMetadataAware {
  val note: BasicNotebook?
  val interpreterCode: String
  var source: String
  val output: NotebookOutput?
  val marker: String

  var text: String

  val indexInNote: Int
  var id: String
  var status: CellStatus

  val isSynced: Boolean

  var config: JsonObject

  var dateUpdated: Date?
  var dateCreated: Date?
  var dateStarted: Date?
  var dateFinished: Date?

  val language: String?
  /**
   * We need special function for set, because it can be invoked not in AWT thread with some delay
   */
  fun changeSyncStatus(newValue: Boolean)

  fun setOutput(notebookOutput: NotebookOutput?)
  fun asJson(): CharSequence
  fun asJsonTree(): JsonObject
  fun setSourceNote(sourceNote: BasicNotebookImpl)
  fun update(cell: NotebookCell)

  /**
   * Get fields which is differ with this cell
   *
   * @return name of fields
   */
  fun getDifference(anotherCell: NotebookCell): Set<String>

  fun copy(): NotebookCell

  fun runWithMuteNotification(runnable: () -> Unit)

  val textRange: TextRange
  val offset: Int

  //Offset without delimiter
  val textOffset: Int
  var title: String?
  var titleVisible: Boolean

  val isLaunched: Boolean
    get() = status == CellStatus.PENDING || status == CellStatus.RUNNING

  fun clear()

  fun isWithoutBody(): Boolean = text.removePrefix(marker).isBlank()
}