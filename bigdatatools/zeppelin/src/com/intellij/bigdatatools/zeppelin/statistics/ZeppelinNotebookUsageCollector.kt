package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.file.getOriginalFile
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

object ZeppelinNotebookUsageCollector : CounterUsagesCollector() {

  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("bigdatatools.zeppelin.notebook", 7)

  private fun getNotebookFields(value: ZeppelinNotebook, addParagraphDetails: Boolean): List<EventPair<out Any?>> {
    val result = mutableListOf(notebook_paragraphs_count.with(value.cells.size), notebook_text_length.with(value.asSource().length),
                               notebook_json_length.with(value.asJson().length))

    if (addParagraphDetails) {
      val res = value.cells.flatMap { it.output?.msg ?: emptyList() }
      val counts = res.groupingBy { it.type }.eachCount()
      appendCounts(result as MutableList<EventPair<*>>, counts)
    }

    return result
  }

  private val notebook_paragraphs_count = EventFields.RoundedInt("notebook_paragraphs_count")
  private val notebook_text_length = EventFields.RoundedInt("notebook_text_length")
  private val notebook_json_length = EventFields.RoundedInt("notebook_json_length")

  private fun getFileTimes(file: VirtualFile): List<EventPair<out Any?>> {
    val currentTimeMillis = System.currentTimeMillis()
    val originFile = file.getOriginalFile()
    val startInitTime = originFile.getUserData(ZeppelinFileLoadStatistic.START_INIT_TIME)
    val fileEditorCreatedTime = originFile.getUserData(ZeppelinFileLoadStatistic.FILE_EDITOR_CREATED)

    val fullLoadTime = if (startInitTime != null) currentTimeMillis - startInitTime else -1
    val editorCreatedTime = if (startInitTime != null && fileEditorCreatedTime != null) fileEditorCreatedTime - startInitTime else -1
    return listOf(full_init_time.with(fullLoadTime), editor_created_time.with(editorCreatedTime))
  }

  private val full_init_time = EventFields.Long("full_init_time")
  private val editor_created_time = EventFields.Long("editor_created_time")

  private fun appendCounts(result: MutableList<EventPair<*>>, counts: Map<CellResultType, Int>?) {
    counts?.forEach { (type, count) ->
      when (type) {
        CellResultType.ANGULAR -> result += resultsCountAngular.with(count)
        CellResultType.HTML -> result += resultsCountHtml.with(count)
        CellResultType.IMG -> result += resultsCountImg.with(count)
        CellResultType.TABLE -> result += resultsCountTable.with(count)
        CellResultType.TEXT -> result += resultsCountText.with(count)
        CellResultType.NETWORK -> result += resultsCountNetwork.with(count)
        CellResultType.NULL -> result += resultsCountNull.with(count)
        CellResultType.SVG -> result += resultsCountSVG.with(count)
        CellResultType.BOKEH_HTML -> result += resultsCountBokehHtml.with(count)
      }
    }
  }

  private val resultsCountAngular = EventFields.RoundedInt("results_count_angular")
  private val resultsCountHtml = EventFields.RoundedInt("results_count_html")
  private val resultsCountImg = EventFields.RoundedInt("results_count_img")
  private val resultsCountTable = EventFields.RoundedInt("results_count_table")
  private val resultsCountText = EventFields.RoundedInt("results_count_text")
  private val resultsCountNetwork = EventFields.RoundedInt("results_count_network")
  private val resultsCountNull = EventFields.RoundedInt("results_count_null")
  private val resultsCountSVG = EventFields.RoundedInt("results_count_svg")
  private val resultsCountBokehHtml = EventFields.RoundedInt("results_count_bokeh_html")

  private val synchronizedEventField = EventFields.Boolean("notebook_synchronized")

  private val countFields = arrayOf<EventField<*>>(resultsCountAngular, resultsCountHtml, resultsCountImg, resultsCountTable,
                                                   resultsCountText, resultsCountNetwork, resultsCountNull, resultsCountSVG,
                                                   resultsCountBokehHtml)

  private val notebookFields = arrayOf<EventField<*>>(notebook_paragraphs_count, notebook_text_length, notebook_json_length) + countFields

  private val actionId = EventFields.Enum<NoteActionsIds>("action_id")
  private val toolbarActionClickedEvent = GROUP.registerVarargEvent("toolbar.click",
                                                                    *(notebookFields + actionId + synchronizedEventField))

  private val noteOpenedEvent = GROUP.registerVarargEvent("note.open",
                                                          *(notebookFields + full_init_time + editor_created_time + synchronizedEventField))

  fun logNoteOpened(project: Project, notebook: ZeppelinNotebook, virtualFile: VirtualFile, synchronized: Boolean) {
    noteOpenedEvent.log(project,
                        getNotebookFields(notebook, addParagraphDetails = true) + getFileTimes(virtualFile) + synchronizedEventField.with(
                          synchronized))
  }

  fun logToolbarActionClickedEvent(project: Project, id: NoteActionsIds, zeppelinNotebook: ZeppelinNotebook, isRemote: Boolean) {
    toolbarActionClickedEvent.log(project, getNotebookFields(zeppelinNotebook, addParagraphDetails = false) + actionId.with(
      id) + synchronizedEventField.with(isRemote))
  }
}