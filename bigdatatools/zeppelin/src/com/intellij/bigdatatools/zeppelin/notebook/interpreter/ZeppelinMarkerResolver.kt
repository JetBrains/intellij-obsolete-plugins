package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw.RawElementTypes
import com.intellij.psi.tree.IElementType
import java.util.concurrent.ConcurrentHashMap

object ZeppelinMarkerResolver {
  private val markerToCodeMap = ConcurrentHashMap<InnerId, Map<String, IElementType?>>()
  private val defaultMarkers = convertInterpreterLanguagePairs(ZeppelinDefaultMarkers.markers)

  fun getMarkerByText(marketText: String?, configId: String?, noteId: String?): IElementType {
    if (marketText == null) return RawElementTypes.marker

    val markerCode = marketText
      .removePrefix(NotebookConstants.PARAGRAPH_DELIMITER.dropLast(1))
      .removePrefix("\n")
      .removePrefix(NotebookConstants.INTERPRETER_MARKER)
      .trim()
      .takeWhile { it != '(' }

    val id = InnerId(configId, noteId)
    val foundMapper = markerToCodeMap[id] ?: markerToCodeMap[id.copy(noteId = null)]

    val mapper = foundMapper ?: defaultMarkers
    return mapper[markerCode] ?: RawElementTypes.marker
  }

  fun getMarkers(configId: String?, noteId: String?): Set<String> {
    val foundMarkers = markerToCodeMap[InnerId(configId, noteId)] ?: markerToCodeMap[InnerId(configId, null)]
    return foundMarkers?.keys ?: emptySet()
  }

  @Suppress("UNCHECKED_CAST")
    /**
     * @return was updated
     */
  fun addExistedInterpreters(interpreterToLanguages: Map<String, String>, configId: String, noteId: String?): Boolean {
    val id = InnerId(configId, noteId)
    val interpreterToCellType = convertInterpreterLanguagePairs(interpreterToLanguages)
    return if (markerToCodeMap[id] != interpreterToCellType) {
      markerToCodeMap[id] = interpreterToCellType
      true
    }
    else
      false
  }

  fun getInterpreterByMarker(cell: NotebookCell, interpreters: List<Interpreter>): Interpreter? {
    val defaultInterpreter = interpreters.firstOrNull() ?: return null

    val markerText = cell.interpreterCode
    val isDefault = markerText.isEmpty() || defaultInterpreter.interpreters.any { markerText == it.name }
    if (isDefault)
      return defaultInterpreter

    val markerGroup = markerText.split(".").first()
    return interpreters.find { it.name == markerGroup }
  }

  private fun convertInterpreterLanguagePairs(interpreterToLanguages: Map<String, String?>): Map<String, IElementType?> =
    interpreterToLanguages.map {
      it.key to (ZeppelinSupportLanguages.langIdToCellMarker[it.value])
    }.toMap()

  fun clearInterpreters(configId: String, noteId: String?) {
    val id = InnerId(configId, noteId)
    markerToCodeMap.remove(id)
  }

  private data class InnerId(val configId: String?, val noteId: String?)
}