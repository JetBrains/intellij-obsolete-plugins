package com.intellij.bigdatatools.zeppelin.components.instance

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtUnexpectedConnectionException
import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInstanceConnectionLogger
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import org.jetbrains.concurrency.AsyncPromise
import java.util.concurrent.TimeUnit

/**
 * Manage connections to Specific Zeppelin Instance
 */
class ZeppelinConnectionManager(val project: Project?,
                                val config: ZeppelinConnectionData) : Disposable {
  private val instanceLock = Any()
  private val noteCachedConnections = mutableMapOf<String, ZeppelinNoteCacheConnection>()
  private val connectionToEditors = mutableMapOf<String, List<NotebookEditor>>()
  val tunnelUri: String?
    get() = instanceConnection.tunnelUri

  val instanceConnection = ZeppelinInstanceCachedConnection(config)

  init {
    Disposer.register(this, instanceConnection)
    Disposer.register(this, ZeppelinInstanceConnectionLogger(instanceConnection))
  }

  override fun dispose() {}

  /**
   * Create cache note connection
   * @param containerId - an id of notebook or null if default
   * @param editor - an editor for which note connection is used, will be disposed with it
   *
   * @return note connection
   */
  fun createNoteConnectionForEditor(containerId: String,
                                    editor: NotebookEditor,
                                    noteId: String = containerId): ZeppelinNoteCacheConnection = synchronized(instanceLock) {
    val cacheConnection = getNoteCachedConnection(containerId) ?: createNoteCachedConnection(containerId, noteId)
    val linkedEditors = connectionToEditors.getOrDefault(containerId, emptyList())
    connectionToEditors += containerId to (linkedEditors + editor)
    return cacheConnection
  }

  fun getNoteCachedConnection(containerId: String) = synchronized(instanceLock) {
    noteCachedConnections[containerId]
  }

  fun destroyNoteCachedConnectionForEditor(containerId: String, editor: NotebookEditor) = synchronized(instanceLock) {
    connectionToEditors[containerId] = connectionToEditors.getOrDefault(containerId, emptyList()) - editor
    if (connectionToEditors[containerId].isNullOrEmpty()) {
      noteCachedConnections[containerId]?.let {
        executeOnPooledThread {
          Disposer.dispose(it)
        }
      }
    }
  }

  fun isConnected() = instanceConnection.isConnected()

  fun refreshConnectionSync(force: Boolean = false) = try {
    asyncRefreshConnection(force).get(ZeppelinTimeouts.CONNECTION_TIMEOUT * 3, TimeUnit.MILLISECONDS)
  }
  catch (t: BdtConnectionException) {
    logger.info(t)
    stopConnection()
    t
  }
  catch (t: Throwable) {
    logger.info(t)
    stopConnection()
    BdtUnexpectedConnectionException(t)
  }


  private var asyncConnectPromise: AsyncPromise<BdtConnectionException?>? = null

  private fun asyncRefreshConnection(force: Boolean = false): AsyncPromise<BdtConnectionException?> = synchronized(this) {
    asyncConnectPromise ?: let {
      val newPromise = BdtAsyncPromise<BdtConnectionException?>()
      asyncConnectPromise = newPromise
      invokeConnectionRefresh(force, newPromise)
      newPromise
    }
  }

  private fun invokeConnectionRefresh(force: Boolean, promise: AsyncPromise<BdtConnectionException?>) = executeOnPooledThread {
    val error = doConnectionRefresh(force)
    onConnected(promise, error)
  }

  private fun onConnected(promise: AsyncPromise<BdtConnectionException?>,
                          error: BdtConnectionException?) = synchronized(this) {
    promise.setResult(error)
    asyncConnectPromise = null
  }

  private fun doConnectionRefresh(force: Boolean): BdtConnectionException? {
    val rootRefreshErrors = try {
      instanceConnection.refreshConnectionSync(force, project)
    }
    catch (e: Exception) {
      BdtUnexpectedConnectionException(e)
    }
    if (rootRefreshErrors != null)
      return rootRefreshErrors

    noteCachedConnections.values.forEach {
      it.refreshConnectionAsync(force, project)
    }
    return null
  }

  private fun stopConnection() = synchronized(this) {
    instanceConnection.disconnect(reason = "Disconnected by ConnectionManager")
    noteCachedConnections.values.forEach {
      it.disconnect(reason = "Disconnected by ConnectionManager")
    }

    asyncConnectPromise?.setError("Disconnected by ConnectionManager")
    asyncConnectPromise = null
  }

  private fun createNoteCachedConnection(containerId: String, noteId: String): ZeppelinNoteCacheConnection {
    val cachedConnection = ZeppelinNoteCacheConnection(instanceConnection, noteId, project)
    Disposer.register(this, cachedConnection)
    Disposer.register(cachedConnection, Disposable {
      synchronized(instanceLock) {
        noteCachedConnections -= containerId
        connectionToEditors[containerId] = emptyList()
      }
    })
    synchronized(instanceLock) {
      noteCachedConnections[containerId] = cachedConnection
    }

    return cachedConnection
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun getNoteConnectionByEditor(zeppelinEditor: ZeppelinEditor): ZeppelinNoteCacheConnection? {
      val file = zeppelinEditor.file
      val project = zeppelinEditor.editor.project ?: return null
      val configId = NotebookFileUtil.getConfigId(file) ?: return null
      val containerId = NotebookFileUtil.getContainerId(file) ?: return null
      val config = ZeppelinDriverManager.getDriver(project, configId)?.connectionData ?: return null
      val connectionManager = ZeppelinDriverManager.getDriver(project, config.innerId)?.connectionManager
      return connectionManager?.getNoteCachedConnection(containerId)
    }
  }
}