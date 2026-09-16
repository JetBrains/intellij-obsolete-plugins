package com.intellij.bigdatatools.zeppelin.components.service

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.interpreter.InterpreterTemplate
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import java.util.concurrent.CopyOnWriteArrayList

class ZeppelinInterpreterSettingsManager(val connection: ZeppelinInstanceCachedConnection) : Disposable {
  private val listeners = CopyOnWriteArrayList<ZeppelinInterpreterSettingsListener>()
  val api get() = connection.api

  val listener = object : ZeppelinConnectionListener {
    override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) = notifyListeners {
      it.updateInterpreterSettings(interpreterSettings)
    }

    override fun updateRepositories(repositories: List<Repository>, requestException: Throwable?) = notifyListeners {
      it.updateRepositories(repositories, requestException)
    }

    override fun updateAvailableInterpreters(interpreters: List<InterpreterSettings>, exception: Throwable?) {
      val templates = interpretersToTemplates(interpreters)
      notifyListeners {
        it.updateAvailableInterpreterTemplates(templates, exception)
      }
    }

    private fun notifyListeners(op: (ZeppelinInterpreterSettingsListener) -> Unit) = listeners.forEach {
      try {
        op(it)
      }
      catch (t: Throwable) {
        logger.error(t)
      }
    }
  }

  init {
    connection.addListener(listener)
  }

  override fun dispose() {
    connection.removeListener(listener)
  }

  fun isRepositoriesAvailable() = connection.isRepositoriesAvailable

  fun getRepos(): List<Repository> {
    val repositoriesException = connection.repositoriesException

    return if (repositoriesException == null)
      connection.repositories
    else
      throw Exception(repositoriesException)
  }

  fun addRepository(repository: Repository) = api.addRepository(repository)
  fun removeRepository(repository: Repository) = api.removeRepository(repository)

  fun getInterpreterTemplates(): List<InterpreterTemplate> {
    val exception = connection.availableInterpretersException
    val interpreters = if (exception == null)
      connection.availableInterpreters
    else
      throw Exception(exception)

    return interpretersToTemplates(interpreters)
  }

  fun isInterpretersSettingsAvailable() = connection.isInterpreterSettingsAvailable
  fun getInterpretersSettings() = connection.interpreterSettings
  fun addInterpreterSettings(interpreterSettings: InterpreterSettings) = api.addInterpreterSettings(interpreterSettings)

  fun updateInterpreterSettings(interpreterSettings: InterpreterSettings): InterpreterSettings? {
    val prepared = interpreterSettings.copy(properties = interpreterSettings.properties.map {
      val name = it.value.name.ifBlank { it.key }
      name to it.value
    }.toMap())
    return api.updateInterpreterSetting(prepared)
  }

  fun removeInterpreterSettings(interpreterSettings: InterpreterSettings) = api.removeInterpreterSettings(interpreterSettings)
  fun restartInterpreterSettings(interpreterSettings: InterpreterSettings) =
    ZeppelinNoteController.restartInterpreterAndNotify(api, null, interpreterSettings.id, interpreterSettings.name)

  fun refreshAsync() = executeOnPooledThread {
    connection.refreshInterpretersSettingsAsync()
    connection.refreshAvailableInterpretersTemplatesAsync()
    connection.updateRepositoriesAsync()
  }

  fun addListener(listener: ZeppelinInterpreterSettingsListener) = listeners.add(listener)
  fun removeListener(listener: ZeppelinInterpreterSettingsListener) = listeners.remove(listener)

  private fun interpretersToTemplates(interpreters: List<InterpreterSettings>): List<InterpreterTemplate> {
    val groupedInterpreters: Map<String, List<InterpreterSettings>> = interpreters.groupBy { it.group }
    return groupedInterpreters.map { (group, interpreters) ->
      val properties = interpreters.flatMap {
        val entries = it.properties.entries
        entries.map { (propertyName, property) -> property.copy(name = propertyName) }
      }
      InterpreterTemplate(group, properties)
    }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun getInstance(project: Project, configId: String) = ZeppelinDriverManager.getDriver(project, configId)?.interpreterSettingsManager
  }
}