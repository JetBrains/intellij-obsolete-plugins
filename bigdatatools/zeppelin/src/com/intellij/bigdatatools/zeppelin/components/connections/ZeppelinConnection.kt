package com.intellij.bigdatatools.zeppelin.components.connections

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtTunnelException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtUnexpectedConnectionException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinBadlyFormedUrlException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinTimeoutException
import com.intellij.bigdatatools.zeppelin.components.connections.components.ZeppelinRestConnectionChecker
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService.createIfRequired
import com.jetbrains.bigdatatools.common.rfs.driver.FailedConnectionStatus
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import org.jetbrains.concurrency.AsyncPromise
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A main class, which represent communication with Remote Zeppelin server
 */
class ZeppelinConnection(
  private val zeppelinConnectionData: ZeppelinConnectionData,
) : Disposable, ZeppelinConnectionProvider {
  private val connectionNotifier = ZeppelinConnectionNotifier()

  val tunnelUri: String?
    get() = innerClient?.tunnel?.tunnelledUri


  private var innerClient: ZeppelinClient? = null
  val api get() = innerClient?.api ?: error("Inner Zeppelin client is not inited")
  val apiOrNull get() = innerClient?.api
  val zeppelinInfo: ZeppelinInfo? get() = innerClient?.api?.innerZeppelinInfo

  init {
    Disposer.register(this, ZeppelinRestConnectionChecker(this))
    Disposer.register(this, KeepAliveService(this))
  }

  override fun dispose() {
    if (isConnected())
      disconnect(null, "Dispose Connection")
  }

  /**
   * Close connection to the Zeppelin
   */
  fun disconnect(statusCode: Int? = null, reason: String? = null) {
    innerClient?.disconnect(statusCode, reason)
    innerClient?.let { Disposer.dispose(it) }
    innerClient = null
  }

  /**
   * Check connection to Zeppelin
   */
  fun getConnectionStatus() = innerClient?.getConnectionStatus() ?: FailedConnectionStatus(
    BdtConnectionException(ZepMessagesBundle.message("error.client.is.not.inited")))


  /**
   * Check connection to Zeppelin
   */
  override fun isConnected(): Boolean = innerClient?.isConnected() ?: false

  private var refreshConnectionPromise: AsyncPromise<BdtConnectionException?>? = null

  fun refreshConnectionSync(force: Boolean = false,
                            project: Project?): BdtConnectionException? {
    val timeout = ZeppelinTimeouts.CONNECTION_TIMEOUT * 2

    return try {
      refreshConnectionAsync(force, project).get(timeout, TimeUnit.MILLISECONDS)
    }
    catch (e: TimeoutException) {
      ZeppelinTimeoutException(zeppelinConnectionData.getNameWithAddress(), timeout, e)
    }
  }

  fun refreshConnectionAsync(force: Boolean = false,
                             project: Project?): AsyncPromise<BdtConnectionException?> = synchronized(this) {
    return refreshConnectionPromise ?: let {
      val newRefreshConnection = BdtAsyncPromise<BdtConnectionException?>()
      refreshConnectionPromise = newRefreshConnection
      executeOnPooledThread {
        val connectionException = doRefreshConnection(force, project)

        synchronized(this) {
          refreshConnectionPromise = null
          newRefreshConnection.setResult(connectionException)
        }
      }
      newRefreshConnection
    }
  }

  /**
   * Connect to server if required
   *
   * @return null or connection error
   */
  private fun doRefreshConnection(force: Boolean = false,
                                  project: Project?): BdtConnectionException? {
    val connected = isConnected()
    when {
      !force && connected -> return null
      force && connected -> disconnect(null, "Force Reconnect")
      else -> disconnect(null, "Reconnecting after disconnect")
    }
    val oldInfo = zeppelinInfo

    try {
      logger.trace("Start connecting to ${zeppelinConnectionData.getFullHttpUrl()} on ${zeppelinConnectionData.getShowedName()}")
      if (zeppelinConnectionData.getFullHttpUrl().isEmpty()) {
        throw ZeppelinBadlyFormedUrlException(zeppelinConnectionData.url)
      }
      val tunnelHandler = try {
        synchronized(zeppelinConnectionData) {
          createIfRequired(project, zeppelinConnectionData.getTunnelData(), zeppelinConnectionData.url, zeppelinConnectionData.innerId)
        }
      }
      catch (t: Throwable) {
        throw BdtTunnelException(t)
      }
      innerClient?.let { Disposer.dispose(it) }
      innerClient = ZeppelinClient(zeppelinConnectionData, connectionNotifier, tunnelHandler, ::addDebugInfo).also {
        Disposer.register(this, it)
      }

      if (oldInfo != innerClient?.api?.innerZeppelinInfo) {
        connectionNotifier.notifyServerInfoChange(innerClient?.api?.innerZeppelinInfo)
      }
      logger.trace("Successfully connected to ${zeppelinConnectionData.getFullHttpUrl()} on ${zeppelinConnectionData.getShowedName()}")
      onConnected()
      return null
    }
    catch (e: BdtConnectionException) {
      logger.trace("Connecting exception to ${zeppelinConnectionData.getFullHttpUrl()} on ${zeppelinConnectionData.getShowedName()}")
      onConnectionError(e)
      return e
    }
    catch (e: Throwable) {
      logger.trace(
        "Unexpected Connecting exception to ${zeppelinConnectionData.getFullHttpUrl()} on ${zeppelinConnectionData.getShowedName()}")
      onConnectionError(BdtUnexpectedConnectionException(e))
      return BdtUnexpectedConnectionException(e)
    }
  }

  private fun onConnected() {
    if (!isConnected()) return
    logger.info("Zeppelin CONNECTION OPEN ${addDebugInfo()}")
    connectionNotifier.notifyOnConnect()
  }

  private fun onConnectionError(throwable: Throwable) {
    if (!isConnected()) return
    logger.info("Zeppelin CONNECTION error  ${addDebugInfo()}", throwable)
    connectionNotifier.notifyOnError(throwable)
  }

  private fun addDebugInfo() = "Url: ${zeppelinConnectionData.url}, id: ${System.identityHashCode(this)}, thread: ${this}"

  override fun addListener(listener: ZeppelinConnectionListener) = connectionNotifier.addListener(listener)
  override fun removeListener(listener: ZeppelinConnectionListener) = connectionNotifier.removeListener(listener)

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}