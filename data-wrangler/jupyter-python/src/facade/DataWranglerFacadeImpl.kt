package com.intellij.dataWrangler.jupyterPython.facade

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.DWMainPanelFactory
import com.intellij.dataWrangler.impl.operations.DWTypeDesc
import com.intellij.dataWrangler.impl.operations.FieldTypeImpl
import com.intellij.dataWrangler.impl.operations.MetaStructImpl
import com.intellij.dataWrangler.impl.view.DATA_WRANGLER_GRID_KEY
import com.intellij.dataWrangler.impl.view.DWMainPanel
import com.intellij.dataWrangler.jupyterPython.actions.DataWranglerOpenAction
import com.intellij.dataWrangler.jupyterPython.actions.DataWranglerOpenTableFileAction
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils.canCreateNotebookContext
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils.canCreateTableFileContext
import com.intellij.dataWrangler.jupyterPython.serialisation.setFields
import com.intellij.dataWrangler.llm.DataWranglerFacade
import com.intellij.dataWrangler.llm.DataWranglerFacade.Status
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.editor.DataGridContainer
import com.intellij.database.run.ui.grid.GridMainPanel
import com.intellij.ide.DataManager
import com.intellij.jupyter.core.jupyter.editor.JupyterFileEditor
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewFileEditor
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

internal class DataWranglerFacadeImpl(private val project: Project) : DataWranglerFacade {

  // ------------------------ openDataWrangler(): Status ------------------------

  override suspend fun openDataWrangler(): Status {
    withContext(Dispatchers.EDT) {
      val state = findOrCreateDataWranglerPanel()
      when (state) {
        is DataWrangler -> {
          val panel = state.panel
          if (!panel.isShowing) {
            panel.showPanel()
          }
          return@withContext Status.SUCCESS
        }
        is AbsentDataWrangler -> when {
          state.dataContext == null -> return@withContext Status.SOURCE_NOT_FOUND
          state.canCreateFromNotebook -> DataWranglerOpenAction.openFromCellWithTable(project, state.dataContext)
          state.canCreateFromTableFile -> DataWranglerOpenTableFileAction.openDataWrangler(project, state.dataContext)
          else -> return@withContext Status.INVALID_SOURCE
        }
      }
    }
    return Status.SUCCESS
  }

  // ------------------------ findDataWrangler(): JsonElement? ------------------------

  override fun findDataWranglerState(): JsonElement {
    val dataWranglerState = findOrCreateDataWranglerPanel()
    return when (dataWranglerState) {
      is AbsentDataWrangler -> buildAbsentDataWranglerObject(dataWranglerState.canCreate)
      is DataWrangler -> {
        val session = findDataWranglerSession(dataWranglerState.panel)
        session?.buildDataWranglerObject() ?: buildAbsentDataWranglerObject()
      }
    }
  }

  private fun buildAbsentDataWranglerObject(canCreate: Boolean = false): JsonObject {
    return buildJsonObject {
      put("state", JsonPrimitive(false))
      put("canCreate", JsonPrimitive(canCreate))
    }
  }

  private fun <C : DataWranglerContext> DataWranglerSession<C>.buildDataWranglerObject(): JsonObject {
    val context = getContext()
    val columnsNames = context.getColumnNames()
    val commandFactories = getEngine().commandsFactories().value
    return buildJsonObject {
      put("state", JsonPrimitive(true))
      put("id", JsonPrimitive(dataWranglerId))
      put("columnNames", columnsNames.buildArray { JsonPrimitive(it) })
      put("commands", commandFactories.buildArray { it.buildCommandObject(context) })
    }
  }

  private fun <C : DataWranglerContext> CommandFactory<*, C>.buildCommandObject(context: DataWranglerContext): JsonObject? {
    val parameters = parametersMetaType.getPrimitiveFields()
    if (parameters == null) {
      logger.warn("Command with unsupported parameters: $id ($commandName)")
      return null
    }
    return buildJsonObject {
      put("id", JsonPrimitive(id))
      put("label", JsonPrimitive(commandName))
      put("description", JsonPrimitive(getDescription()))
      put("parameters", parameters.buildArray { it.buildParameterObject(context) })
    }
  }

  private fun DWTypeDesc.DWStructTypeDesc.Field<*, *>.buildParameterObject(context: DataWranglerContext): JsonObject {
    return buildJsonObject {
      put("id", JsonPrimitive(id))
      put("label", JsonPrimitive(desc.defaultName))
      put("typeName", desc.targetType?.buildTypeNamePrimitive() ?: JsonNull)
      put("possibleValues", desc.possibleValues?.getValues(context)?.buildArray { it.buildValueObject() } ?: JsonNull)
    }
  }

  // Utils

  private inline fun <T : Any> Collection<T>.buildArray(jsonBuilder: (T) -> JsonElement?): JsonArray =
    JsonArray(mapNotNull { jsonBuilder.invoke(it) }.toList())

  private fun <V : Any> KClass<V>.buildTypeNamePrimitive(): JsonPrimitive {
    return JsonPrimitive(java.name)
  }

  private fun <V : Any> V.buildValueObject(): JsonObject {
    val value = this
    val displayName = if (value is DisplayName) value.getDisplayName() else null
    return buildJsonObject {
      put("value", value.buildValuePrimitive())
      put("label", JsonPrimitive(displayName))
    }
  }

  private fun <V : Any> V.buildValuePrimitive(): JsonPrimitive {
    if (this is Enum<*>) return JsonPrimitive(this.name)
    val value = takeIf { it::class.isStringOrPrimitive }
    return when (value) {
      is String -> JsonPrimitive(value)
      is Boolean -> JsonPrimitive(value)
      is Number -> JsonPrimitive(value)
      else -> JsonNull
    }
  }

  private val KClass<*>.isStringOrPrimitive: Boolean
    get() = this == String::class || java.isPrimitive


  // ------------------------ applyDataWranglerSolution(solution: JsonElement): Status ------------------------

  override suspend fun applyDataWranglerSolution(solution: JsonElement): Status {
    val dataWranglerObject = solution.jsonObject
    val dataWranglerId = dataWranglerObject["id"]!!.jsonPrimitive.content
    val session = withContext(Dispatchers.EDT) {
      val panel = findDataWranglerPanel() ?: return@withContext null
      if (!panel.isShowing) {
        panel.showPanel()
      }
      findDataWranglerSession(panel)
    }
    if (session == null) return Status.SOURCE_NOT_FOUND
    if (session.dataWranglerId != dataWranglerId) return Status.INVALID_SOURCE
    return session.applyDataWranglerSolution(dataWranglerObject)
  }

  private suspend fun <C : DataWranglerContext> DataWranglerSession<C>.applyDataWranglerSolution(dataWranglerObject: JsonObject): Status {
    val steps = createSteps(dataWranglerObject) ?: return Status.FAILURE
    runTransformations(steps)
    return Status.SUCCESS
  }

  private fun <C : DataWranglerContext> DataWranglerSession<C>.createSteps(dataWranglerObject: JsonObject): List<TransformationStep<*, C>>? {
    val commandObjects = dataWranglerObject["commands"]!!.jsonArray.map { it.jsonObject }
    val steps = mutableListOf<TransformationStep<*, C>>()
    for (commandObject in commandObjects) {
      val step = createStep(commandObject)
      if (step == null) return null
      steps.add(step)
    }
    return steps
  }

  private fun <C : DataWranglerContext> DataWranglerSession<C>.createStep(commandObject: JsonObject): TransformationStep<*, C>? {
    val commandId = commandObject["id"]!!.jsonPrimitive.content
    val commandFactory = getEngine().commandsFactories().value.find { it.id == commandId } ?: return null
    val parameterObjects = commandObject["parameters"]!!.jsonArray.map { it.jsonObject }
    return createStep(commandFactory, parameterObjects)
  }

  private fun <P : Any, C : DataWranglerContext> createStep(
    commandFactory: CommandFactory<P, C>,
    parameterObjects: List<JsonObject>,
  ): TransformationStep<P, C>? {
    val parameterObjectMap = createParameterValuesMap(parameterObjects)
    val parametersMetaType = commandFactory.parametersMetaType
    val isVariantParametersMetaType = parametersMetaType.isVariantType()
    val parameters = parametersMetaType.getNewInstance()
    try {
      parameters.setFields((parametersMetaType as MetaStructImpl<P>).desc, {
        val fieldId = if (isVariantParametersMetaType) it.substringAfter('.') else it
        parameterObjectMap[fieldId]
      })
    }
    catch (_: Exception) {
      return null
    }
    return TransformationStep(commandFactory, parameters)
  }

  private fun createParameterValuesMap(parameterObjects: List<JsonObject>): Map<String, Any?> {
    return parameterObjects.associate { parameterObject ->
      val id = parameterObject["id"]!!.jsonPrimitive.content
      val value = parameterObject["value"]!!.jsonPrimitive.content
      val typeName = parameterObject["typeName"]!!.jsonPrimitive.content
      val typedValue = value.tyTypedValueOrNull(typeName)
      id to typedValue
    }
  }

  private fun String.tyTypedValueOrNull(typeName: String): Any? {
    try {
      val clazz = Class.forName(typeName)
      if (clazz.isEnum) {
        return clazz.enumConstants.find { (it as Enum<*>).name == this }
      }
    }
    catch (_: ClassNotFoundException) {}

    return when (typeName) {
      String::class.java.name -> this
      Boolean::class.java.name -> toBooleanStrictOrNull()
      Byte::class.java.name -> toByteOrNull()
      Short::class.java.name -> toShortOrNull()
      Int::class.java.name -> toIntOrNull()
      Long::class.java.name -> toLongOrNull()
      Float::class.java.name -> toFloatOrNull()
      Double::class.java.name -> toDoubleOrNull()
      else -> null
    }
  }


  // ------------------------ findDataGrid(): Object? ------------------------

  override fun findDataGrid(): Object? {
    val dataGridInfo = findDataGridInfo(searchNested = false) ?: return null
    return dataGridInfo.grid as Object
  }


  // ------------------------ Utils -----------------------

  private fun <P : Any> MetaStruct<P>.getPrimitiveFields(): List<DWTypeDesc.DWStructTypeDesc.Field<*, *>>? {
    val fields = getFields().values.mapNotNull { (it as? FieldTypeImpl)?.fieldDesc }

    if (fields.all { it.desc is DWTypeDesc.DWPrimitiveTypeDesc<*> }) return fields

    val variantFieldDesc = fields.singleOrNull()?.desc as? DWTypeDesc.DWVariantTypeDesc<*>
    if (variantFieldDesc != null) {
      val mainVariant = variantFieldDesc.variants.firstOrNull() ?: return null
      val mainVariantFields = mainVariant.desc.fields
      if (mainVariantFields.all { it.desc is DWTypeDesc.DWPrimitiveTypeDesc<*> }) return mainVariantFields
      return null
    }

    return null
  }

  private fun <P : Any> MetaStruct<P>.isVariantType(): Boolean {
    val fields = getFields().values.mapNotNull { (it as? FieldTypeImpl)?.fieldDesc }
    val variantFieldDesc = fields.singleOrNull()?.desc as? DWTypeDesc.DWVariantTypeDesc<*>
    return variantFieldDesc != null
  }


  private data class DataGridInfo(val grid: DataGrid, val isNested: Boolean)

  private fun findDataGridInfo(searchNested: Boolean): DataGridInfo? {
    val fileEditor = FileEditorManager.getInstance(project).selectedEditor ?: return null

    val grid = when (fileEditor) {
      is DataViewFileEditor -> fileEditor.dataViewerPanel.gridMutableStateFlow.value
      is DataGridContainer -> fileEditor.dataGrid
      else -> null
    }
    grid?.let { return DataGridInfo(it, isNested = false) }

    if (!searchNested) return null
    val nestedGrid = when (fileEditor) {
      is JupyterFileEditor -> {
        val editor = fileEditor.editor
        val tables = UIUtil.findComponentsOfType(editor.component, GridMainPanel::class.java)
        val singleTable = tables.singleOrNull() ?: return null
        singleTable.grid
      }
      else -> null
    }
    nestedGrid?.let { return DataGridInfo(it, isNested = true) }

    return null
  }

  private sealed interface DataWranglerState
  private data class DataWrangler(val panel: DWMainPanel) : DataWranglerState
  private data class AbsentDataWrangler(
    val dataContext: DataContext? = null,
    val canCreateFromNotebook: Boolean = false,
    val canCreateFromTableFile: Boolean = false,
  ) : DataWranglerState {
    val canCreate: Boolean
      get() = canCreateFromNotebook || canCreateFromTableFile
  }

  private fun findOrCreateDataWranglerPanel(): DataWranglerState {
    return findDataWranglerPanel(createIfAbsent = true)
  }

  private fun findDataWranglerPanel(): DWMainPanel? {
    val dataWranglerState = findDataWranglerPanel(createIfAbsent = false) as? DataWrangler ?: return null
    return dataWranglerState.panel
  }

  private fun findDataWranglerPanel(createIfAbsent: Boolean): DataWranglerState {
    // Nested grids cannot have an existing Data Wrangler panel
    val gridInfo = findDataGridInfo(searchNested = createIfAbsent) ?: return AbsentDataWrangler()
    val grid = gridInfo.grid
    val isNested = gridInfo.isNested

    val dataWranglerPanel = grid.getUserData(DATA_WRANGLER_GRID_KEY)
    if (dataWranglerPanel != null) return DataWrangler(dataWranglerPanel)

    if (!createIfAbsent) return AbsentDataWrangler()
    val dataContext = DataManager.getInstance().getDataContext(grid.panel.component)
    val newDataWranglerPanel = tryCreateDataWranglerPanel(dataContext, grid, isNested)
    if (newDataWranglerPanel != null) return DataWrangler(newDataWranglerPanel)

    return AbsentDataWrangler(
      dataContext,
      canCreateNotebookContext(dataContext),
      canCreateTableFileContext(dataContext)
    )
  }

  private fun tryCreateDataWranglerPanel(dataContext: DataContext, grid: DataGrid, isNested: Boolean): DWMainPanel? {
    if (isNested) return null // We want to create a Data Wrangler panel for the grid opened in a separate tab
    return DWMainPanelFactory.createMainPanel(grid, dataContext, showAfterCreation = false)
  }

  private fun findDataWranglerSession(panel: DWMainPanel): DataWranglerSession<*>? {
    val dataContext = DataManager.getInstance().getDataContext(panel)
    val session = dataContext.getData(DW_SESSION) ?: return null
    return session
  }

  private val DataWranglerSession<*>.dataWranglerId: String
    get() = "${System.identityHashCode(this)}"


  companion object {
    private val logger = logger<DataWranglerFacadeImpl>()
  }
}