package com.intellij.bigdatatools.zeppelin.ztools.controller

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.ztools.collector.ZtoolsRefSqlTableInfo
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsCodeGenerator.PYTHON_ZTOOLS_WARNING_HEADER
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsCodeGenerator.SCALA_ZTOOLS_WARNING_HEADER
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugTask
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase

interface ZtoolsService {
  fun processInfo(project: Project,
                  file: NotebookVirtualFile,
                  config: ZeppelinConnectionData,
                  json: String,
                  interpreterCode: String,
                  zeppelinEditor: ZeppelinEditor)

  fun isInterpreterSupported(debugCellTask: ZtoolsDebugTask): Boolean

  fun getServiceCellText(project: Project, language: String, params: UserDataHolderBase, ztoolsConfig: ZtoolsConfig): String?

  fun initProject(project: Project) {}

  companion object {
    private const val ID: String = "com.intellij.bigdatatools.zeppelin.ztools"
    private val EP_NAME = ExtensionPointName.create<ZtoolsService>(ID)

    val NOTE_FILE = Key<NotebookVirtualFile>("NOTE_FILE")
    val INTERPRETER_GROUP = Key<String>("INTERPRETER_CODE")
    val DEFINED_TABLES = Key<List<ZtoolsRefSqlTableInfo>>("ALLOWED_SQL_TABLE_NAMES")
    val ALLOWED_DATAFRAME_NAMES_ID = Key<List<String>>("ALLOWED_DATAFRAME_NAMES_ID")
    val IS_ON_DEMAND = Key<Boolean>("RUN_ON_DEMAND")
    val FORCE_IGNORE_SQL = Key<Boolean>("FORCE_IGNORE_SQL")

    fun isZtoolsCellSupported(debugCellTask: ZtoolsDebugTask) = extensions.any { it.isInterpreterSupported(debugCellTask) }

    fun getZtoolsCellText(project: Project,
                          file: NotebookVirtualFile,
                          language: String,
                          params: UserDataHolderBase,
                          config: ZtoolsConfig): String {
      val serviceCellTexts = extensions.mapNotNull { it.getServiceCellText(project, language, params, config) }
      if (serviceCellTexts.isEmpty())
        return ""

      val debugInfo = ZtoolsNoteController.getFor(file)?.debugInfo
      debugInfo?.sourceCodes = serviceCellTexts

      return (when (language) {
                "scala" -> SCALA_ZTOOLS_WARNING_HEADER
                "python" -> PYTHON_ZTOOLS_WARNING_HEADER
                else -> ""
              } + serviceCellTexts.joinToString("\n"))
    }


    fun handleZtoolsOutput(project: Project,
                           file: NotebookVirtualFile,
                           config: ZeppelinConnectionData,
                           result: String,
                           interpreterCode: String,
                           zeppelinEditor: ZeppelinEditor) = extensions.forEach { integration ->
      integration.processInfo(project, file, config, result, interpreterCode,zeppelinEditor)
    }

    fun initForProject(project: Project) = extensions.forEach {
      it.initProject(project)
    }

    private val extensions: List<ZtoolsService>
      get() = EP_NAME.extensionList
  }
}