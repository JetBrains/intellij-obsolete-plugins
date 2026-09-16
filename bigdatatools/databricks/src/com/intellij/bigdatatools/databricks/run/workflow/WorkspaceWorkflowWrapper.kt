package com.intellij.bigdatatools.databricks.run.workflow

import com.databricks.sdk.service.workspace.ExportFormat
import com.databricks.sdk.service.workspace.ImportFormat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ResourceUtil
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

internal class WorkspaceWorkflowWrapper(
  private val project: Project,
  private val dataManager: DatabricksDataManager,
) {
  fun createPyFileWrapper(rfsPath: RfsPath): RfsPath {
    val wrapperText = ResourceUtil.getResourceAsStream(this::class.java.classLoader,
                                                       "wrapper",
                                                       "file.workflow-wrapper.py").reader().readText()
    return initFile(rfsPath, wrapperText, ImportFormat.AUTO)
  }

  fun createIpynbWrapper(virtualFile: VirtualFile, originalPath: RfsPath): RfsPath {
    val text = dataManager.client.workspaceExport(originalPath.stringRepresentation().removeSuffix(".py"), ExportFormat.JUPYTER)
    val originalNote = JsonParser.parseString(text).asJsonObject

    val wrapperCellString = ResourceUtil.getResourceAsStream(this::class.java.classLoader,
                                                             "wrapper",
                                                             "notebook.workflow-wrapper.py").reader().readText()

    val wrapperCell = JsonObject()
    wrapperCell.addProperty("cell_type", "code")
    wrapperCell.add("source", JsonArray().also { it.add(wrapperCellString) })
    wrapperCell.add("metadata", JsonObject())
    wrapperCell.add("outputs", JsonArray())
    wrapperCell.add("execution_count", JsonPrimitive(1))

    val cells = listOf(wrapperCell) + originalNote.get("cells").asJsonArray
    val prepared = cells.map { cell ->
      cell as JsonObject
      val source = cell.get("source")
      val newSource = if (source.isJsonArray)
        source.asJsonArray.flatMap { it.asString.trimEnd().replace("\r", "").split("\n") }
      else
        listOf(source.asString)

      val isCode = cell.get("cell_type").asString == "code"
      Cell(newSource, isCode, cell)
    }

    val rearranged = rearrangeCells(prepared).mapNotNull { cell ->
      if (cell.isCode) {
        val dbJupyterCell: JsonObject = cell.original ?: wrapperCell
        val newCell = dbJupyterCell.deepCopy()
        newCell.add("source", JsonArray().also { it.add(cell.source.joinToString("\n")) })
        newCell
      }
      else
        null
    }

    val newCells = JsonArray()
    rearranged.forEach { cell ->
      newCells.add(cell)
    }
    originalNote.add("cells", newCells)
    val preparedNoteString = originalNote.toString()

    return initFile(originalPath, preparedNoteString, ImportFormat.JUPYTER)
  }

  private fun initFile(originalPath: RfsPath, text: String, format: ImportFormat): RfsPath {
    val extensionIndex = originalPath.name.lastIndexOf('.')
    var extension = if (extensionIndex > 0) {
      originalPath.name.substring(extensionIndex)
    }
    else ""
    if (format == ImportFormat.JUPYTER)
      extension = extension.removeSuffix(".py")
    val copyPath = originalPath.parent!!.addRelative("${originalPath.name}.databricks.workflow-wrapper${extension}", isDirectory = false)
    createFile(copyPath, text, originalPath, format)

    return copyPath
  }

  private fun createFile(targetPath: RfsPath, text: String, originalPath: RfsPath, format: ImportFormat) {
    val projectPath = dataManager.syncManager.getTaskForProject(project).syncMapper.baseRemotePath

    val prepared = text
      .replace("{{DATABRICKS_SOURCE_FILE}}", "/Workspace" + originalPath.stringRepresentation())
      .replace("{{DATABRICKS_PROJECT_ROOT}}", "/Workspace" + projectPath.stringRepresentation())
    dataManager.saveTextFile(targetPath, prepared, format)
  }

  private fun rearrangeCells(cells: List<Cell>): Array<Cell> {
    val beginningCells: MutableList<Cell> = mutableListOf()
    val endingCells: MutableList<Cell> = mutableListOf()

    for (cell in cells) {
      if (!cell.isCode) {
        endingCells.add(cell)
        continue
      }

      val newCell = Cell(
        source = mutableListOf(),
        isCode = true,
        original = cell.original,
      )

      for (line in cell.source) {
        if (line.startsWith("%pip install") || line.startsWith("# MAGIC %pip install")) {
          beginningCells.add(
            Cell(
              source = mutableListOf(
                "import os",
                "os.chdir(os.path.dirname('{{DATABRICKS_SOURCE_FILE}}'))",
                line,
              ),
              isCode = true,
              original = null
            )
          )
          continue
        }
        else if (line.startsWith("dbutils.library.restartPython()")) {
          beginningCells.add(Cell(source = mutableListOf(line), isCode = true, original = null))
          continue
        }
        newCell.source += line
      }

      endingCells.add(newCell)
    }

    return (beginningCells + endingCells).filter { it.source.isNotEmpty() }.toTypedArray()
  }

  private inner class Cell(var source: List<String>, val isCode: Boolean, val original: JsonObject?)
}