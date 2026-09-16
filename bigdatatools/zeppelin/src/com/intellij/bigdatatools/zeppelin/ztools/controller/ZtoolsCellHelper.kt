package com.intellij.bigdatatools.zeppelin.ztools.controller

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.ztools.collector.ZtoolsCollectorUtils
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugTask
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase

object ZtoolsCellHelper {
  private const val ztoolsIdScalaPrefix = "//ZToolsId = "
  private const val ztoolsIdPythonPrefix = "#ZToolsId = "

  fun getDebugCellText(config: ZtoolsConfig,
                       debugCellTask: ZtoolsDebugTask,
                       project: Project,
                       file: NotebookVirtualFile): String {
    val language = debugCellTask.language
    val interpreterCode = debugCellTask.interpreterCode
    val qualifiedMarker = "${NotebookConstants.INTERPRETER_MARKER}${interpreterCode}"

    val sqlTableNames = if (!debugCellTask.forceIgnoreSql && config.sqlSettings.isEnabled)
      ZtoolsCollectorUtils.collectTableNames(project, file)
    else
      emptyList()

    val variableSettings = config.variablesSettings
    val dfNames = if (variableSettings.isEnabled && variableSettings.sameNoteOnly)
      ZtoolsCollectorUtils.collectDeclNames(language, project, file)
    else
      null

    val params = UserDataHolderBase().also {
      it.putUserData(ZtoolsService.NOTE_FILE, file)
      it.putUserData(ZtoolsService.INTERPRETER_GROUP, debugCellTask.interpreterGroup)
      it.putUserData(ZtoolsService.DEFINED_TABLES, sqlTableNames)
      it.putUserData(ZtoolsService.ALLOWED_DATAFRAME_NAMES_ID, dfNames?.toList())
      it.putUserData(ZtoolsService.IS_ON_DEMAND, debugCellTask.onDemand)
      it.putUserData(ZtoolsService.FORCE_IGNORE_SQL, debugCellTask.forceIgnoreSql)
    }

    val innerText = ZtoolsService.getZtoolsCellText(project, file, language, params, config)
    if (innerText.isBlank())
      return ""

    val cellText = "$qualifiedMarker\n$innerText"
    val marker = cellText.substringBefore("\n")
    val afterMarket = cellText.substringAfter("\n")
    val idPrefix = getIdPrefix(debugCellTask.language)

    return "$marker\n$idPrefix${debugCellTask.debugId}\n${afterMarket}"
  }

  private fun getIdPrefix(language: String) = when (language) {
    "scala" -> ztoolsIdScalaPrefix
    "python" -> ztoolsIdPythonPrefix
    else -> error("Unsupported StateViewer language $language")
  }
}