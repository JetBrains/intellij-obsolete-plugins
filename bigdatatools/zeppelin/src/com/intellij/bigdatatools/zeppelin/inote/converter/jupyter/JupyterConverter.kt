package com.intellij.bigdatatools.zeppelin.inote.converter.jupyter

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.JupyterCell
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.JupyterCellOutput
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.JupyterCellOutputType
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.JupyterCellType
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.JupyterNote
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.JupyterOutputType
import com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model.ZeppelinResultGenerator
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCellBuilder
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebookBuilder
import com.intellij.openapi.diagnostic.Logger

object JupyterConverter {
  private val logger = Logger.getInstance(this::class.java)

  fun fromJupyterToZeppelin(jupyterNoteString: String): ZeppelinNotebook {
    //In Databricks the server can sent multiple fields with the same name, we cannot parse it by moshi
    val cleared = removeDoubleFields(jupyterNoteString)
    val jupyterNote = BdtJson.fromJsonToClass(cleared, JupyterNote::class.java)
    return fromJupyterToZeppelin(jupyterNote)
  }

  private fun fromJupyterToZeppelin(jupyterNote: JupyterNote): ZeppelinNotebook {
    val name = jupyterNote.metadata["title"] as? String ?: "Note converted from Jupyter"

    val tempNotebook = ZeppelinNotebookBuilder.createNotebook(name = name, id = "", cells = emptyList())

    var zepCells: List<ZeppelinCell> = emptyList()
    tempNotebook.performModification {
      zepCells = jupyterNote.cells.map {
        fromJupCellToZeppelinCell(tempNotebook, it)
      }
    }
    return ZeppelinNotebookBuilder.createNotebook(name = name, id = "", zepCells)
  }


  fun fromZeppelinToJupyter(zepNote: ZeppelinNotebook): String {
    val jupyterCells = zepNote.cells.withIndex().map { (index, paragraph) ->
      convertZeppelinCellToJupyter(paragraph, index)
    }
    val jupyterNote = JupyterNote(
      nbformat_minor = 2,
      nbformat = 4,
      cells = jupyterCells,
      metadata = getJupyterMeta(zepNote)
    )

    return BdtJson.toJson(jupyterNote, pretty = true, indent = " ")
  }

  private fun getJupyterMeta(zepNote: ZeppelinNotebook): Map<String, Any> {
    val kernelSpec = mapOf("language" to "scala",
                           "name" to "spark2-scala")
    val langInfo = mapOf(
      "codemirror_mode" to "text/x-scala",
      "file_extension" to ".scala",
      "mimetype" to "text/x-scala",
      "name" to "scala",
      "pygments_lexer" to "scala",
    )
    return mapOf(
      "name" to zepNote.name,
      "kernelspec" to kernelSpec,
      "language_info" to langInfo
    )
  }

  private fun convertZeppelinCellToJupyter(paragraph: ZeppelinCell,
                                           index: Int): JupyterCell {
    val code: String = paragraph.text.trimStart()
    return when {
      code.startsWith("%md") -> {
        JupyterCell(cellType = JupyterCellType.MARKDOWN,
                    metadata = emptyMap(),
                    source = code.removePrefix("%md"))
      }
      code.startsWith("%sql") || code.startsWith("%html") -> {
        JupyterCell(cellType = JupyterCellType.CODE,
                    metadata = emptyMap(),
                    source = "%$code",
                    executionCount = index,
                    outputs = emptyList())
      }
      else -> {
        JupyterCell(cellType = JupyterCellType.CODE,
                    metadata = mapOf("autoscroll" to "auto"),
                    executionCount = index,
                    source = code,
                    outputs = emptyList())
      }
    }
  }

  private fun removeDoubleFields(jupyterNoteString: String): String {
    val gson = Gson()
    val jsonTree = gson.fromJson(jupyterNoteString, JsonObject::class.java)
    return gson.toJson(jsonTree)
  }

  private fun fromJupCellToZeppelinCell(zepNotebook: ZeppelinNotebook, jupyterCell: JupyterCell): ZeppelinCell {
    val codeSource = getText(jupyterCell.source)

    val interpreterCode = when (jupyterCell.cellType) {
      JupyterCellType.CODE -> "%python\n"
      JupyterCellType.MARKDOWN -> "%md\n"
      else -> ""
    }

    val cellText = if (jupyterCell.cellType == JupyterCellType.MARKDOWN)
      interpreterCode + codeSource
    else
      codeSource

    val zeppelinCell = ZeppelinCellBuilder.createFromText(zepNotebook, text = cellText)
    val zepOutput = when (jupyterCell.cellType) {
      JupyterCellType.CODE -> codeCellOutput(jupyterCell)
      JupyterCellType.MARKDOWN, JupyterCellType.HEADING -> {
        zeppelinCell.setUpMarkdownConfig(true)
        //Render Markdown here
        val msg = CellResultMessage(CellResultType.TEXT, codeSource)
        NotebookOutput(OutputCode.SUCCESS, listOf(msg))
      }
      else -> NotebookOutput(OutputCode.SUCCESS, listOf())
    }
    zeppelinCell.setOutput(zepOutput)
    return zeppelinCell
  }

  private fun codeCellOutput(jupyterCell: JupyterCell): NotebookOutput? {
    if (jupyterCell.outputs == null)
      return null
    val zepOutputs = jupyterCell.outputs.flatMap { cellOutput ->
      when (cellOutput.outputType) {
        JupyterCellOutputType.EXECUTION_RESULT -> convertOutputMessages(cellOutput)
        JupyterCellOutputType.DISPLAY_DATA -> convertOutputMessages(cellOutput)
        JupyterCellOutputType.ERROR -> listOf(
          CellResultMessage(CellResultType.TEXT, getText(listOfNotNull(cellOutput.ename, cellOutput.evalue))))
        JupyterCellOutputType.STREAM -> listOf(CellResultMessage(CellResultType.TEXT, cellOutput.text?.let { getText(it) } ?: ""))
        else -> {
          logger.error("Unrecognized Jupyter type ${cellOutput.outputType}.\n It be converted to text. Output\n '${cellOutput}'")
          listOf(CellResultMessage(CellResultType.TEXT, cellOutput.toString()))
        }
      }
    }

    return NotebookOutput(OutputCode.SUCCESS, zepOutputs)
  }

  private fun convertOutputMessages(cellOutput: JupyterCellOutput): List<CellResultMessage> =
    cellOutput.data?.entries?.map { (key, value) -> getZeppelinOutputMessageFromData(key, value) } ?: emptyList()

  private fun getOutputType(outputKey: String) =
    JupyterOutputType.entries.firstOrNull { it.type == outputKey } ?: JupyterOutputType.TEXT_PLAIN

  private fun getZeppelinOutputMessageFromData(key: String, data: Any): CellResultMessage {
    val type = getOutputType(key)
    val outputData = getText(data)

    return when {
      type === JupyterOutputType.IMAGE_PNG -> {
        val base64Code = outputData.replace("\n", "")
        CellResultMessage(type.zeppelinType, ZeppelinResultGenerator.toBase64ImageHtmlElement(base64Code))
      }
      type === JupyterOutputType.LATEX -> {
        CellResultMessage(type.zeppelinType, ZeppelinResultGenerator.toLatex(outputData))
      }
      type === JupyterOutputType.APPLICATION_JAVASCRIPT -> {
        CellResultMessage(type.zeppelinType, ZeppelinResultGenerator.toJavascript(outputData))
      }
      else -> {
        CellResultMessage(type.zeppelinType, outputData)
      }
    }
  }


  private fun getText(source: Any): String {
    val sourceRows = when (source) {
      is String -> listOf(source)
      is List<*> -> source.map {
        it.toString()
      }
      else -> emptyList()
    }
    return verifyEndOfLine(sourceRows).joinToString(separator = "") { it }
  }

  private fun verifyEndOfLine(content: List<String>): List<String> {
    if (content.size == 1) {
      // one-liners don't have line separator
      return content
    }

    return content.withIndex().map {
      val line = it.value
      val i = it.index
      if (!line.endsWith("\n") && i != content.size - 1)
        line + "\n"
      else
        line
    }
  }
}

