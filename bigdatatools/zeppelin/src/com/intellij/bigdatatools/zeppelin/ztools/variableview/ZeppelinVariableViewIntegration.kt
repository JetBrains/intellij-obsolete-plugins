package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsCodeGenerator
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsService
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugTask
import com.intellij.bigdatatools.zeppelin.ztools.inlays.ZtoolsInlaysService
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase

class ZeppelinVariableViewIntegration : ZtoolsService {
  @Suppress("UNCHECKED_CAST")
  override fun processInfo(project: Project,
                           file: NotebookVirtualFile,
                           config: ZeppelinConnectionData,
                           json: String,
                           interpreterCode: String,
                           zeppelinEditor: ZeppelinEditor) {
    val jsonValue = json.split("---ztools-scala---").getOrNull(1) ?: return
    val jsonMap = JsonParser.fromJsonToMap(jsonValue)

    val debugInfo = ZtoolsNoteController.getFor(file)?.debugInfo
    debugInfo?.collectedOutputs?.add(jsonMap)

    val variables = jsonMap["variables"] as? Map<String, Any> ?: emptyMap()
    val errors = jsonMap["errors"] as? List<String>
    val variableViewManager = VariableViewManager.getInstance(project)
    variableViewManager.setVariables(file, variables, interpreterCode, false)
    variableViewManager.addErrors(file, interpreterCode, errors)

    ZtoolsInlaysService.getFor(zeppelinEditor)?.invokeUpdate()
  }

  override fun getServiceCellText(project: Project, language: String, params: UserDataHolderBase, ztoolsConfig: ZtoolsConfig): String? {
    if (!ztoolsConfig.variablesSettings.isEnabled)
      return null

    val isOnDemand = params.getUserData(ZtoolsService.IS_ON_DEMAND)
    val conf = ztoolsConfig.variablesSettings
    if (isOnDemand == false && conf.isOnDemandOnly)
      return null

    val allowedNames = params.getUserData(ZtoolsService.ALLOWED_DATAFRAME_NAMES_ID)

    return when (language) {
      "scala" -> ZtoolsCodeGenerator.getCollectScalaDataframes(depth = conf.depth,
                                                               enableProfiling = true,
                                                               collectionSizeLimit = conf.collectionSizeLimit,
                                                               stringSizeLimit = conf.stringSizeLimit,
                                                               timeout = conf.timeout,
                                                               variableTimeout = conf.variableTimeout,
                                                               interpreterResCountLimit = conf.interpreterResCountLimit,
                                                               filterNames = allowedNames)
      "python" -> ZtoolsCodeGenerator.getCollectPythonDataframes(depth = conf.depth,
                                                                 collectionSizeLimit = conf.collectionSizeLimit,
                                                                 stringSizeLimit = conf.stringSizeLimit,
                                                                 timeout = conf.timeout,
                                                                 filterNames = allowedNames)
      else -> null
    }
  }

  override fun isInterpreterSupported(debugCellTask: ZtoolsDebugTask): Boolean =
    debugCellTask.interpreterGroup == "spark" && debugCellTask.subInterpreter in listOf("pyspark", "spark")
}