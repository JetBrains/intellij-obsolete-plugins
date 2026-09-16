package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.jupyter.nbformat.JupyterCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.bash.BashTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database.hive.HiveElementTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database.sparksql.SparkSqlElementTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.markdown.MarkdownElementTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw.RawElementTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala.SparkElementTypes
import com.intellij.psi.tree.IElementType

/**
 * Provide markers, sources. types and derivatives for all implemented languages
 */
internal object ZeppelinSupportLanguages {
  val languageTypes = listOf(
    BashTypes,
    SparkElementTypes,
    HiveElementTypes,
    SparkSqlElementTypes,
    MarkdownElementTypes,
    RawElementTypes,
    /*ZeppelinPythonElementTypes,
    KotlinElementTypes*/
  )
  val CELL_TYPES = languageTypes.map { it.cellType } + JupyterCellType.CODE_OR_MAGIC

  val markerToCellTypes: Map<IElementType, NotebookCellType> = languageTypes.map { it.marker to it.cellType }.toMap()
  val markerToIds: Map<IElementType, String> = languageTypes.map { it.marker to it.id }.toMap()
  val markerToSources: Map<IElementType, IElementType> = languageTypes.map { it.marker to it.source }.toMap()
  val idToTypes: Map<String, InterpreterTypes> = languageTypes.map { it.id to it }.toMap()

  val cellTypesToSources = languageTypes.map { it.cellType to it.source }.toMap() +
                           mapOf(JupyterCellType.CODE_OR_MAGIC to ZeppelinTypes.CODE_SOURCE,
                                 JupyterCellType.MAGIC to ZeppelinTypes.RAW_SOURCE)

  val langIdToCellMarker = languageTypes.map { it.id to it.marker }.toMap()
}