// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.models.notebook

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookCellUtil
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookCellBase
import com.intellij.bigdatatools.notebooks.core.impl.serialize.NotebookImportDeserializer
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.openapi.diagnostic.Logger
import java.text.ParseException
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class ZeppelinCell(zeppelinNotebook: ZeppelinNotebook,
                   override val json: JsonObject) : NotebookCellBase(zeppelinNotebook) {
  private val schema = NotebookSchema

  override val textOffset: Int
    get() = offset + NotebookConstants.PARAGRAPH_DELIMITER.length

  override val note: ZeppelinNotebook
    get() = sourceNotebook as ZeppelinNotebook

  val user: String?
    get() = json["user"]?.asString
  val jobName: String?
    get() = json["jobName"]?.asString

  override var id: String by Delegates.observable(json[NotebookSchema.cellId]?.asString ?: "") { _, old, new ->
    if (new == old) return@observable
    notifyChangeFields(listOf(NotebookSchema.cellId))
    json.add(NotebookSchema.cellId, JsonPrimitive(new))
  }


  private var innedIsSynced = AtomicBoolean(true)


  override val isSynced: Boolean
    get() = innedIsSynced.get()

  override fun changeSyncStatus(newValue: Boolean) {
    val oldValue = innedIsSynced.getAndSet(newValue)
    if (oldValue != newValue) {
      notifyChangeFields(listOf(NotebookSchema.isSynced))
    }
  }

  override val language: String?
    get() = config.get("language")?.asString

  override var dateUpdated: Date?
    get() = parseDate("dateUpdated")
    set(value) {
      if (value == dateUpdated) return
      setDate("dateUpdated", value)
      notifyChangeFields(listOf("dateUpdated"))
    }

  override var dateCreated: Date?
    get() = parseDate("dateCreated")
    set(value) {
      if (value == dateCreated) return
      setDate("dateCreated", value)
      notifyChangeFields(listOf("dateCreated"))
    }
  override var dateStarted: Date?
    get() = parseDate("dateStarted")
    set(value) {
      if (value == dateStarted) return
      setDate("dateStarted", value)
      notifyChangeFields(listOf("dateStarted"))
    }

  override var dateFinished: Date?
    get() = parseDate("dateFinished")
    set(value) {
      if (value == dateFinished) return
      setDate("dateFinished", value)
      notifyChangeFields(listOf("dateFinished"))
    }

  val progressUpdateIntervalMs: Int
    get() = json["progressUpdateIntervalMs"]?.asInt ?: 0
  val focus: Boolean
    get() = json["focus"]?.asBoolean ?: false

  override var config: JsonObject
    get() = json[NotebookSchema.cellConfig]?.asJsonObject ?: let {
      val newConf = JsonObject()
      json.add(NotebookSchema.cellConfig, newConf)
      newConf
    }
    set(value) {
      json.add(NotebookSchema.cellConfig, value)
      if (json.get(NotebookSchema.cellConfig) != value)
        notifyChangeFields(listOf(NotebookSchema.cellConfig))
    }

  override var title: String?
    get() {
      val jsonElement = json[NotebookSchema.title] ?: return null
      return if (!jsonElement.isJsonNull)
        jsonElement.asString
      else
        null
    }
    set(value) {
      if (title == value) return

      notifyChangeFields(listOf(NotebookSchema.title))
      json.addProperty(NotebookSchema.title, value)
    }

  override var titleVisible: Boolean
    get() = config[NotebookSchema.titleVisible]?.asBoolean ?: false
    set(value) {
      if (titleVisible == value) return

      notifyChangeFields(listOf(NotebookSchema.cellConfig))
      config.addProperty(NotebookSchema.titleVisible, value)
    }

  var tableHide: Boolean
    get() = config[NotebookSchema.tableHide]?.asBoolean ?: false
    set(value) {
      if (tableHide == value) return

      notifyChangeFields(listOf(NotebookSchema.tableHide, NotebookSchema.cellConfig))
      config.addProperty(NotebookSchema.tableHide, value)
    }

  var editorHide: Boolean
    get() = config[NotebookSchema.editorHide]?.asBoolean ?: false
    set(value) {
      if (editorHide == value) return

      notifyChangeFields(listOf(NotebookSchema.editorHide, NotebookSchema.cellConfig))
      config.addProperty(NotebookSchema.editorHide, value)
    }

  var settings: ParagraphSettings by Delegates.observable(getSettingsFromJson()) { _, old, new ->
    if (old == new) return@observable

    notifyChangeFields(listOf(NotebookSchema.cellSettings))
    json.add(NotebookSchema.cellSettings, Gson().toJsonTree(new))
  }

  override var status: CellStatus
    get() = json[NotebookSchema.cellStatus]?.asString?.let {
      CellStatus.valueOf(it)
    } ?: CellStatus.READY
    set(value) {
      if (value == status) return
      json.addProperty(NotebookSchema.cellStatus, value.name)
      notifyChangeFields(listOf(NotebookSchema.cellStatus))
    }

  override val output: NotebookOutput?
    get() = results

  override var text: String by Delegates.observable(json[NotebookSchema.cellText]?.asString ?: "") { _, old, new ->
    if (old == new) return@observable
    marker = NotebookCellUtil.getMarkerText(text)
    interpreterCode = NotebookCellUtil.calcInterpreterCode(marker)
    notifyChangeFields(listOf(NotebookSchema.cellText))
    json.add(NotebookSchema.cellText, JsonPrimitive(new))
  }

  override var marker: String = NotebookCellUtil.getMarkerText(text)
    private set

  override var interpreterCode: String = calcInterpreterCode()
    private set

  override fun update(cell: NotebookCell) {
    cell as ZeppelinCell

    val allKeys = cell.json.keySet() + json.keySet()

    val changedFields = allKeys.filter {
      json.get(it) != cell.json.get(it)
    }.toMutableList()

    //Text can be empty of null, so we need to additionally check it
    if (text == cell.text)
      changedFields -= schema.cellText


    val set = json.keySet().toTypedArray()
    set.forEach { json.remove(it) }
    cell.asJsonTree().entrySet().forEach { json.add(it.key, it.value) }

    //Update cached values
    id = cell.id
    source = cell.source
    settings = cell.settings
    text = cell.text
    results = cell.results
    config = cell.config
    changeSyncStatus(isSynced)

    notifyChangeFields(changedFields)
  }

  override var source: String by Delegates.observable(NotebookCellUtil.toSource(text))
  { _, old, new ->
    if (old == new) return@observable
    notifyChangeFields(listOf(NotebookSchema.cellSource))
    val newText = NotebookCellUtil.toText(new)
    text = newText
  }

  private var results: NotebookOutput? by Delegates.observable(
    json[NotebookSchema.results]?.toString()?.let {
      JsonParser.parseStringJsonToObject(it, NotebookOutput::class.java)
    }) { _, old, new ->
    if (old == new) return@observable
    notifyChangeFields(listOf(NotebookSchema.results))
    if (new != null)
      json.add(NotebookSchema.results, Gson().toJsonTree(new))
    else
      json.remove(NotebookSchema.results)
  }

  fun setUpMarkdownConfig(toActiveEditOnDblClickMode: Boolean) {
    val editorSetting: MutableMap<String, Any> = HashMap()
    editorSetting["language"] = "markdown"
    editorSetting["editOnDblClick"] = toActiveEditOnDblClickMode
    config.addProperty("editorHide", toActiveEditOnDblClickMode)
    config.add("editorSetting", JsonObject())
    config.addProperty("editorMode", "ace/mode/markdown")
  }

  override fun setOutput(notebookOutput: NotebookOutput?) {
    results = notebookOutput
  }

  override fun asJsonTree() = json
  override fun asJson(): CharSequence = json.toString()

  override fun getMetadata(): JsonObject? = Gson().toJsonTree(settings.params).asJsonObject.get(
    NotebookSchema.cellMetadata)?.asJsonObject

  override fun createMetadata(): JsonObject {
    val metadata = JsonObject()
    updateMetaData(metadata)
    return getMetadata()!!
  }

  override fun setMetadata(key: String, value: JsonElement) {
    val metadata = getOrCreateMetadata()
    metadata.add(key, value)
    updateMetaData(metadata)
  }

  override fun removeMetadata(key: String) {
    val metadata = getOrCreateMetadata()
    metadata.remove(key)
    updateMetaData(metadata)
  }

  override fun copy(): ZeppelinCell = ZeppelinCell(sourceNotebook as ZeppelinNotebook, asJsonTree().deepCopy()).also {
    it.runWithMuteNotification {
      it.changeSyncStatus(isSynced)
    }
  }

  override fun clear() {
    source = NotebookCellUtil.toSource("")
    setOutput(null)
    title = null
    editorHide = false
    tableHide = false
    status = CellStatus.READY
    getMetadata()?.keySet()?.forEach {
      removeMetadata(it)
    }
  }

  private fun getSettingsFromJson(): ParagraphSettings {
    val jsonSettings = json[NotebookSchema.cellSettings]
    return jsonSettings?.toString()?.let {
      JsonParser.parseStringJsonToObject(it, ParagraphSettings::class.java)
    } ?: let {
      val paragraphSettings = ParagraphSettings()
      val gson = Gson()
      json.add(NotebookSchema.cellSettings, gson.toJsonTree(paragraphSettings))
      paragraphSettings
    }
  }

  private fun updateMetaData(metadata: JsonObject) {
    notifyChangeFields(listOf(NotebookSchema.cellMetadata))
    val metaMap = JsonParser.fromJsonToMap(metadata.toString())
    val newParams = settings.params + mapOf(NotebookSchema.cellMetadata to metaMap)
    settings = settings.copy(params = newParams)
  }

  private fun parseDate(dateField: String): Date? {
    return try {
      val dateString = json[dateField] ?: return null
      gson.fromJson(dateString, Date::class.java)
    }
    catch (e: ParseException) {
      logger.error("Failed parse $dateField", e)
      null
    }
  }

  private fun setDate(dateField: String, date: Date? = Date()) {
    json.add(dateField, gson.toJsonTree(date))
  }

  private fun calcInterpreterCode() = marker
    .removePrefix(NotebookConstants.INTERPRETER_MARKER)
    .trim()


  companion object {
    private val logger = Logger.getInstance(this::class.java)
    private val gson = GsonBuilder()
      .setPrettyPrinting()
      .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
      .registerTypeAdapter(Date::class.java, NotebookImportDeserializer())
      .create()
  }
}