package com.intellij.bigdatatools.zeppelin.api.remote.rest

import com.intellij.bigdatatools.zeppelin.components.connections.parser.ZeppelinVersionAdapter
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryProxyProtocol
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinCredentials
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinFullTextSearchElement
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinSearchResponse
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.credentialStore.Credentials
import org.apache.http.client.utils.URIBuilder

/**
 * The service to work with Zeppelin by the RESt API
 *
 * @param restApi - REST service
 */
class ZeppelinRestApi(private val restApi: ZeppelinRestClient) {

  /**
   * Create a notebook
   *
   * @param notebook - a model of new notebook
   *
   * @return the id of the created notebook
   */
  fun createNotebook(notebook: ZeppelinNotebook): String {
    val json = JsonParser.fromJsonToMap(notebook.json.toString())
    val result = restApi.performPostData("/notebook", json, "Create notebook")
    return result.body as String
  }

  /**
   * Delete a notebook by Notebook Id
   *
   * @param noteId - an id of the notebook
   */
  @Suppress("unused")
  fun deleteNotebook(noteId: String) {
    restApi.performDeleteData("/notebook/$noteId", "Delete Notebook")
  }

  /**
   * Stop executing paragraphs in a notebook
   *
   * @param noteId - an id of a notebook
   */
  fun stopAllParagraphs(noteId: String) {
    restApi.performDeleteData("/notebook/job/$noteId/", "Stop a notebook job")
  }

  /**
   * Get list of all notebooks on Zeppelin
   */
  fun getNotebooks(zeppelinInfo: ZeppelinInfo): List<NotebookInfo> {
    val result = restApi.performGetRequest("/notebook", "Get notebook list")
    return ZeppelinVersionAdapter.getInstance(zeppelinInfo).parseNotebookInfos(result.body)
  }

  /**
   * Get a notebook
   *
   * @param notebookId - an id of the notebook
   */
  fun getNotebook(notebookId: String): Any {
    val result = restApi.performGetRequest("/notebook/$notebookId", "Get notebook")
    return result.body
  }

  /**
   * Login to Zeppelin server
   *
   * @param user - login/password of an user
   */
  fun login(user: Credentials): ZeppelinCredentials {
    val params = mapOf(
      "userName" to (user.userName ?: ""),
      "password" to (user.getPasswordAsString() ?: ""))

    val result = restApi.performPostForm("/login", params, "Login")
    return JsonParser.fromValueObject(result.body, ZeppelinCredentials::class.java)
  }

  /**
   * Get list of all interpreter settings
   */
  fun getInterpreterSettings(parser: ZeppelinVersionAdapter): List<InterpreterSettings> {
    val result = restApi.performGetRequest("/interpreter/setting", "Get interpreter settings")
    return parser.parseInterpretersSettings(result.body)
  }

  /**
   * Get list of all interpreter settings
   */
  fun getInterpreters(parser: ZeppelinVersionAdapter): List<InterpreterSettings> {
    val result = restApi.performGetRequest("/interpreter", "Get interpreters")

    val interpreters = (result.body as Map<*, *>).values.toList()
    return parser.parseInterpreters(interpreters)
  }

  /**
   * Restart an interpreter
   *
   * @param interpreterId - an id of the interpreter
   * @param noteId - an id of notebook for which interpreter will be restarted
   */
  fun restartInterpreter(interpreterId: String, noteId: String?) {
    val data: Map<String, String> = noteId?.let { mapOf("noteId" to noteId) } ?: mapOf()
    restApi.performPutData("/interpreter/setting/restart/${interpreterId}",
                           data, "Restart interpreter")
  }

  /**
   * Update an interpreter bindings for the notebook
   * @param interpreterSettings - an interpreter settings
   */
  fun addInterpreterSetting(interpreterSettings: InterpreterSettings,
                            adapter: ZeppelinVersionAdapter): InterpreterSettings {
    val newData = JsonParser.fromObjectToMap(adapter.encodeInterpreterSettings(interpreterSettings))
    val result = restApi.performPostData("/interpreter/setting/${interpreterSettings.id}",
                                         newData, "Add interpreter ${interpreterSettings.name}(${interpreterSettings.id}) settings")
    return adapter.parseInterpreterSettings(result.body)
  }


  /**
   * Update an interpreter bindings for the notebook
   * @param interpreterSettings - an interpreter settings
   */
  fun updateInterpreterSetting(interpreterSettings: InterpreterSettings,
                               adapter: ZeppelinVersionAdapter): InterpreterSettings {
    val newData = prepareInterpreterSettingsForUpdate(adapter, interpreterSettings)
    val result = restApi.performPutData("/interpreter/setting/${interpreterSettings.id}",
                                        newData, "Update interpreter ${interpreterSettings.name}(${interpreterSettings.id}) settings")
    return adapter.parseInterpreterSettings(result.body)
  }

  fun removeInterpreterSetting(interpreterSettings: InterpreterSettings) {
    restApi.performDeleteData("/interpreter/setting/${interpreterSettings.id}",
                              "Remove interpreter ${interpreterSettings.name}(${interpreterSettings.id}) settings")
  }

  /**
   * Clone a notebook
   *
   * @param originalNoteId - a notebook which must be cloned
   * @param newNotebookName - a name of notebook copy
   */
  fun cloneNote(originalNoteId: String, newNotebookName: String): String {
    val result = restApi.performPostData("/notebook/$originalNoteId",
                                         mapOf("name" to newNotebookName), "Clone notebook")
    return result.body as String
  }

  fun renameNote(noteId: String, newNotebookName: String) {
    restApi.performPutData("/notebook/$noteId/rename",
                           mapOf("name" to newNotebookName), "Rename notebook")
  }

  /**
   * Get Zeppelin version from the server
   */
  fun getZeppelinInfo(): ZeppelinInfo? {
    val result = restApi.performGetRequest("/version", "Get version")
    return ZeppelinVersionAdapter.parseVersion(result.body)
  }

  /**
   * Import the notebook to the server
   *
   * @param noteJson - note Json
   *
   * @return note id
   */
  fun importNotebook(noteJson: String): String {
    val data = JsonParser.fromJsonToMap(noteJson)
    val result = restApi.performPostData("/notebook/import", data, "Import Notebook")
    return result.body as String
  }

  /**
   * Export the notebook from the server
   *
   * @param noteId - note Json
   *
   * @return notebook json
   */
  fun exportNotebook(noteId: String): String {
    val result = restApi.performGetRequest("/notebook/export/$noteId", "Export Notebook")
    return result.body as String
  }

  fun getRepositories(zeppelinInfo: ZeppelinInfo): List<Repository> {
    val result = restApi.performGetRequest("/interpreter/repository", "Get Interpreter Repositories")
    return ZeppelinVersionAdapter.getInstance(zeppelinInfo).parseRepositories(result.body)
  }

  fun removeRepository(repository: Repository) {
    restApi.performDeleteData("/interpreter/repository/${repository.id}", "Remove Repository")
  }


  fun addRepository(repository: Repository) {
    val data = mapOf(
      "id" to repository.id,
      "url" to repository.url,
      "snapshot" to repository.snapshot,
      "username" to repository.authentication?.username,
      "password" to repository.authentication?.password.toString(),
      "proxyProtocol" to (repository.proxy?.type ?: RepositoryProxyProtocol.HTTP).name,
      "proxyHost" to (repository.proxy?.host ?: ""),
      "proxyPort" to repository.proxy?.port,
      "proxyLogin" to (repository.proxy?.auth?.username ?: ""),
      "proxyPassword" to ((repository.proxy?.auth?.password?.toString() ?: ""))
    )
    restApi.performPostData("/interpreter/repository", data, "Add Interpreter Repository")
  }

  fun fullTextSearch(query: String): List<ZeppelinFullTextSearchElement> {
    val ub = URIBuilder("/notebook/search")
    ub.addParameter("q", query)
    val result = restApi.performGetRequest(ub.toString(), "Full Text Search")
    return JsonParser.fromValueList(result.body, ZeppelinSearchResponse::class.java).map {
      ZeppelinFullTextSearchElement(id = it.id, fileName = it.fileName, query = query, name = it.name, snippet = it.snippet, text = it.text,
                                    connId = it.connId, header = it.header, fileInfo = null)
    }
  }

  /**
   * Not all settings can be updated by UPDATE request!
   */
  @Suppress("UNCHECKED_CAST")
  private fun prepareInterpreterSettingsForUpdate(adapter: ZeppelinVersionAdapter,
                                                  interpreterSettings: InterpreterSettings): Map<String, Any> {
    val encodedInterpreterSettings = adapter.encodeInterpreterSettings(interpreterSettings)
    val data = JsonParser.fromObjectToMap(encodedInterpreterSettings).filter {
      it.key in setOf("option", "properties", "dependencies")
    } as Map<String, Any>
    val properties = adapter.encodeProperties(interpreterSettings)
    val filteredProperties = properties.filter {
      it.key !in setOf("zeppelin.interpreter.localRepo", "zeppelin.interpreter.output.limit")
    }

    return data + mapOf("properties" to filteredProperties)
  }
}