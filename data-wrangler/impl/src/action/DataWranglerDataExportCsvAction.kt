package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.database.csv.CsvFormats
import com.intellij.database.datagrid.GridHelper
import com.intellij.database.datagrid.GridUtil
import com.intellij.database.dump.ExtractionHelper.FileExtractionHelper
import com.intellij.database.extractors.ExtractionConfigBuilder
import com.intellij.database.extractors.FormatExtractorFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataWranglerDataExportCsvAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    setDataWranglerActionState(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val session = e.getData(DW_SESSION) ?: return
    if (e.project == null) return
    val context = session.getContext()
    val parentDir = context.getFile()?.parent ?: context.getProject().getBaseDirectories().firstOrNull() ?: return
    val format = FormatExtractorFactory(CsvFormats.CSV_FORMAT.getValue())
    val targetExtension = format.fileExtension
    val grid = GridUtil.getDataGrid(e.dataContext) ?: return
    val source = GridHelper.get(grid).createDumpSource(grid, e) ?: return
    e.coroutineScope.launch(Dispatchers.Default) {
      val fileWrapper = withContext(Dispatchers.EDT) {
        val descriptor = FileSaverDescriptor(templateText, "", targetExtension)
        val chooser: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, context.getProject())
        val tableName = session.getContext().getTableName().subSequence(2, session.getContext().getTableName().length - 2)
        chooser.save(parentDir, "$tableName.$targetExtension")
      } ?: return@launch
      val outputFile = fileWrapper.getVirtualFile(true) ?: return@launch
      withContext(Dispatchers.EDT) {
        GridHelper.get(grid).createDumpHandler(source, FileExtractionHelper(outputFile.toNioPath().toFile()), format,
                                               ExtractionConfigBuilder().apply { setAddColumnHeader(true) }.build()).performDump()
      }
    }
  }
}