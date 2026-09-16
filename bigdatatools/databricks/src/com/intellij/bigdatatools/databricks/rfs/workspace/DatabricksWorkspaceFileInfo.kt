package com.intellij.bigdatatools.databricks.rfs.workspace

import com.databricks.sdk.service.workspace.ExportFormat
import com.databricks.sdk.service.workspace.ImportFormat
import com.databricks.sdk.service.workspace.Language
import com.databricks.sdk.service.workspace.ObjectInfo
import com.databricks.sdk.service.workspace.ObjectType
import com.intellij.bigdatatools.coreUi.util.DriverException
import com.intellij.bigdatatools.coreUi.util.prefixIfNot
import com.intellij.bigdatatools.databricks.model.DbExportFormat
import com.intellij.bigdatatools.databricks.rfs.utils.DbRfsUtils
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.bigdatatools.common.rfs.driver.*
import com.jetbrains.bigdatatools.common.rfs.driver.task.RemoteFsDeleteTask
import com.jetbrains.bigdatatools.common.rfs.driver.task.RfsCopyMoveTask
import com.jetbrains.bigdatatools.common.rfs.fileInfo.LazyFileInfoInputStream
import java.io.InputStream
import java.util.Base64

internal class DatabricksWorkspaceFileInfo(override val driver: DatabricksWorkspaceDriver,
                                  val info: ObjectInfo) : FileInfoBase() {
  private val client = driver.client

  override val path: RfsPath = let {
    val clearPath = info.path.removePrefix("/").removeSuffix("/")
    val stringPath = clearPath + if (info.objectType == ObjectType.DIRECTORY) "/" else ""
    driver.createRfsPath(stringPath)
  }

  override val isCopySupport: Boolean = info.objectType == ObjectType.DIRECTORY || info.objectType == ObjectType.NOTEBOOK
  override val externalPath: String = "workspace:/" + path.stringRepresentation()
  override val name: String = path.name
  override val length: Long = -1
  override val modificationTime: Long = 1

  override fun nameForDriver(driver: Driver, exportFormat: com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat?) = when {
    driver is DatabricksWorkspaceDriver -> name
    isFile && exportFormat != null -> name + exportFormat.extension
    else -> name
  }

  override fun getCopyFormatsFor(targetDriver: Driver): List<com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat> {
    if (targetDriver != driver) {
      val noteLanguage = info.language ?: return emptyList()

      val codeExtension = DbRfsUtils.getExtensionByDbNoteLanguage(noteLanguage)
      return listOfNotNull(
        SOURCE_EXPORT_FORMAT.copy(extension = codeExtension),
        HTML_EXPORT_FORMAT,
        JUPYTER_EXPORT_FORMAT.takeIf { noteLanguage == Language.PYTHON },
        DBC_EXPORT_FORMAT)
    }
    else
      return emptyList()
  }

  override fun doRenameAsync(newPath: RfsPath, overwrite: Boolean): RfsCopyMoveTask =
    throw Exception("Operation is not supported by Databricks API")

  override fun doDeleteAsync() = object : RemoteFsDeleteTask(path) {
    override fun run(indicator: ProgressIndicator) {
      val stringPath = path.stringRepresentation().prefixIfNot("/")
      client.workspaceDelete(stringPath, recursive = true)
    }
  }

  override fun doGetReadStream(offset: Long, exportFormat: com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat?) = object : LazyFileInfoInputStream() {
    override fun initStream(): InputStream {
      val stringPath = path.stringRepresentation().prefixIfNot("/")
      val fileInfo = client.workspaceFileInfo(stringPath) ?: throw DriverException(
        DatabricksBundle.message("error.workspace.object.info.not.found", stringPath))
      if (fileInfo.objectType != ObjectType.NOTEBOOK)
        throw DriverException(DatabricksBundle.message("error.notebook.expected", fileInfo.objectType))

      val realExportFormat = exportFormat
      val dbExportFormat = exportFormatToDbFormat(realExportFormat)

      val base64 = client.workspaceExport(stringPath, dbExportFormat)
      val bytes = Base64.getDecoder().decode(base64)

      return if (realExportFormat?.id == INOTE_EXPORT_FORMAT_ID) {
        //val noteText = DbcToZeppelinConverter.readNoteText(bytes)
        //val zepNote = DbcToZeppelinConverter.transformToZeppelin(noteText)
        //zepNote.asJson().byteInputStream()
        bytes.inputStream()
      }
      else
        bytes.inputStream()
    }
  }

  companion object {
    const val INOTE_EXPORT_FORMAT_ID = "Zeppelin"

    val DBC_EXPORT_FORMAT = ExportFormat(DbExportFormat.DBC.name, DatabricksBundle.message("note.format.dbc"), ".dbc")
    val JUPYTER_EXPORT_FORMAT = ExportFormat(DbExportFormat.JUPYTER.name, DatabricksBundle.message("note.format.jupyter"), ".ipynb")
    val HTML_EXPORT_FORMAT = ExportFormat(DbExportFormat.HTML.name, DatabricksBundle.message("note.format.html"), ".html")
    val SOURCE_EXPORT_FORMAT = ExportFormat(DbExportFormat.SOURCE.name, DatabricksBundle.message("note.format.source"), "")

    fun exportFormatToDbFormat(format: com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat?) = when (format?.id) {
      null -> ExportFormat.DBC
      INOTE_EXPORT_FORMAT_ID -> ExportFormat.DBC
      DbExportFormat.SOURCE.name -> ExportFormat.SOURCE
      DbExportFormat.HTML.name -> ExportFormat.HTML
      DbExportFormat.JUPYTER.name -> ExportFormat.JUPYTER
      DbExportFormat.DBC.name -> ExportFormat.DBC
      else -> error("Wrong export format $format")
    }

    fun exportFormatToDbImportFormat(format: com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat?) = when (format?.id) {
      null -> ImportFormat.DBC
      INOTE_EXPORT_FORMAT_ID -> ImportFormat.DBC
      DbExportFormat.SOURCE.name -> ImportFormat.SOURCE
      DbExportFormat.HTML.name -> ImportFormat.HTML
      DbExportFormat.JUPYTER.name -> ImportFormat.JUPYTER
      DbExportFormat.DBC.name -> ImportFormat.DBC
      else -> error("Wrong export format $format")
    }


    fun exportFormatByExtension(extension: String) = when {
      extension.isBlank() -> null
      extension == "ipynb" -> JUPYTER_EXPORT_FORMAT
      extension == "html" -> HTML_EXPORT_FORMAT
      extension == "dbc" -> DBC_EXPORT_FORMAT
      extension in setOf("scala", "py", "r", "sql") -> SOURCE_EXPORT_FORMAT.copy(extension = ".$extension")
      else -> null
    }
  }
}