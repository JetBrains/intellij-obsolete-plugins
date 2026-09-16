package com.intellij.bigdatatools.visualization.statistics

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.statistics.AllowedList
import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponent
import com.intellij.bigdatatools.visualization.inlays.NotebookInlaySettings
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

internal object VisualizationChangesCollector : CounterUsagesCollector() {

  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("bigdatatools.visualization", 3)

  private val CELL_INDEX = EventFields.RoundedInt("cell_index")
  private val CELL_TYPE = EventFields.String("cell_type", AllowedList.cellCodeAllowedList)

  private val INLAY_COLLAPSED = EventFields.Boolean("collapsed")

  private val TABLE_COLUMNS = EventFields.RoundedInt("table_columns")
  private val TABLE_ROWS = EventFields.RoundedInt("table_rows")

  private val NOTEBOOK_PARAGRAPHS_COUNT = EventFields.RoundedInt("notebook_paragraphs_count")
  private val NOTEBOOK_TEXT_LENGTH = EventFields.RoundedInt("notebook_text_length")
  private val NOTEBOOK_JSON_LENGTH = EventFields.RoundedInt("notebook_json_length")

  private val START_INIT_TIME = Key<Long>("startInitTime")

  fun logVisualizationChangeEvent(inlayComponent: NotebookInlayComponent, inlaySettings: NotebookInlaySettings) {
    val tableDimensions = inlayComponent.getTableDimensions()
    visualizationChangeEvent.log(
      CELL_INDEX.with(inlayComponent.cell.indexInNote),
      CELL_TYPE.with(AllowedList.cellInterpreterCode(inlayComponent.cell)),
      INLAY_COLLAPSED.with(inlaySettings.collapsed ?: false),
      TABLE_COLUMNS.with(tableDimensions?.first ?: -1),
      TABLE_ROWS.with(tableDimensions?.second ?: -1)
    )
  }

  private val visualizationChangeEvent = GROUP.registerVarargEvent("visualization.change",
                                                                   CELL_INDEX,
                                                                   CELL_TYPE,
                                                                   INLAY_COLLAPSED,
                                                                   TABLE_COLUMNS,
                                                                   TABLE_ROWS)

  fun logVisualizationLoadEvent(basicNotebook: BasicNotebook, value: VirtualFile, loadDuration: Long) {
    val originFile = if (value is NotebookVirtualFile) value.originalFile else value
    val startInitTime = originFile.getUserData(START_INIT_TIME) ?: return

    visualizationLoadEvent.log(NOTEBOOK_PARAGRAPHS_COUNT.with(basicNotebook.cells.size),
                               NOTEBOOK_TEXT_LENGTH.with(basicNotebook.asSource().length),
                               NOTEBOOK_JSON_LENGTH.with(basicNotebook.asJson().length),
                               INLAYS_FULL_LOAD_DURATION.with(System.currentTimeMillis() - startInitTime),
                               INLAYS_SELF_LOAD_DURATION.with(loadDuration))
  }

  private val INLAYS_FULL_LOAD_DURATION = EventFields.Long("inlays_full_load_duration")
  private val INLAYS_SELF_LOAD_DURATION = EventFields.Long("inlays_self_load_duration")

  /** Called when notebook loaded. */
  private val visualizationLoadEvent = GROUP.registerVarargEvent("visualization.load",
                                                                 NOTEBOOK_PARAGRAPHS_COUNT,
                                                                 NOTEBOOK_TEXT_LENGTH,
                                                                 NOTEBOOK_JSON_LENGTH,
                                                                 INLAYS_FULL_LOAD_DURATION,
                                                                 INLAYS_SELF_LOAD_DURATION)
}