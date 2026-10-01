package com.intellij.aidebugger.common.services.network.client

import com.intellij.aidebugger.common.services.network.DebuggerTransport
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

/**
 * Simple implementation of DebuggerTransport that connects to a socket
 * and reads lines from it.
 */
class DebuggerSimpleClientTransport(
    val config: DebuggerClientTransportConfig,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : DebuggerTransport<String> {

    companion object {
        const val HANDSHAKE_TIMEOUT_IN_MILLIS = 5000
        const val CONNECTION_TIMEOUT_IN_MILLIS = 5000

        private val logger = thisLogger()
    }

    private val _isRunning = AtomicBoolean(false)

    private val _isConnected = MutableStateFlow(false)

    private var _socket: Socket? = null

    private val socket: Socket
        get() = _socket ?: error("Socket is not initialized")

    private var reader: BufferedReader? = null

    override val name: String = "AI Debugger client transport (url: ${config.url})"

    override val incoming: Channel<String> = Channel(Channel.BUFFERED)

    init {
        if (config.autoConnect) {
            scope.launch(Dispatchers.IO) {
                start()
            }
        }
    }

    override val isConnected: StateFlow<Boolean>
        get() = _isConnected

    override suspend fun start() {
        logger.info("$name. Transport start connection")

        if (_isRunning.getAndSet(true)) {
            logger.info("$name. Transport is already running. Skip connection logic.")
            return
        }

        connectWithRetries(
            host = config.host,
            port = config.port,
            retriesCount = config.maxRetriesCount,
            retryDelay = config.retryDelay
        )

        _isConnected.value = true

        startReading()
    }

    override suspend fun stop() {
        if (!_isRunning.getAndSet(false)) {
            logger.info("$name. Transport has been already stopped")
            return
        }

        closeConnection()
    }

    //region Private Methods

    private suspend fun connectWithRetries(
        host: String,
        port: Int,
        retriesCount: Int,
        retryDelay: Duration
    ) {

        for (retryIndex in 1..retriesCount) {

            logger.debug { "[$retryIndex/${retriesCount}] $name. Start new connection attempt" }

            try {
                connectTransportOnce(host, port)?.let { createdSocket ->
                    _socket = createdSocket
                    return
                }
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: IOException) {
                logger.debug(e) { "[$retryIndex/${retriesCount}] $name. Connection attempt failed: ${e.message}" }
            }
            catch (e: Exception) {
                logger.error("[$retryIndex/${retriesCount}] $name. Connection attempt terminated due to unexpected error: ${e.message}", e)
                throw e
            }

            delay(retryDelay)
        }

        logger.info("$name. Failed to connect after <$retriesCount> attempts")
    }

    /**
     * Attempts to establish a single connection to the specified host and port.
     * Sets up a socket configuration, handles timeouts, and logs the connection status.
     *
     * @return A connected socket instance if the connection is successful, or null if it fails.
     * @throws IOException If an I/O error occurs during the connection attempt.
     */
    private fun connectTransportOnce(
        host: String,
        port: Int
    ): Socket? {
        try {
            logger.debug { "$name. Trying to connect..." }

            val clientSocket = Socket()
            clientSocket.soTimeout = HANDSHAKE_TIMEOUT_IN_MILLIS
            clientSocket.connect(
                InetSocketAddress(host, port),
                CONNECTION_TIMEOUT_IN_MILLIS
            )

            clientSocket.soTimeout = 0

            logger.info("$name. Connected successfully")
            return clientSocket
        }
        catch (e: IOException) {
            logger.debug(e) { "$name. Connection failed with error: ${e.message}" }
            return null
        }
    }

    private fun closeConnection() {
        logger.debug { "$name. Transport is closing connection" }

        try {
            reader?.close()
            _socket?.close()
            incoming.close()
        } catch (e: Exception) {
            logger.error("$name. Error closing connection: ${e.message}", e)
        } finally {
            reader = null
            _socket = null
            _isConnected.value = false
            _isRunning.set(false)
        }
    }

    private fun startReading() = scope.launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        logger.info("$name. Start reading from the input stream")

        check(isConnected.value) {
            "$name. Transport is not connected yet. Please make sure you call connect() method first"
        }

        try {
            // Check if socket is still available before trying to read
            val currentSocket = _socket
            if (currentSocket == null || currentSocket.isClosed) {
                logger.warn("$name. Socket is null or closed, cannot start reading")
                println("$name. Socket is null or closed, cannot start reading")
                return@launch
            }

            reader = currentSocket.getInputStream().bufferedReader()

            while (_isRunning.get() && _socket != null && !_socket!!.isClosed) {
                val line = reader?.readLine() ?: break
                incoming.send(line)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("$name. Error in transport operation: ${e.message}", e)
        } finally {
            closeConnection()
        }
    }

    //endregion Private Methods
}