package com.intellij.aidebugger.common.services.network.server

import com.intellij.aidebugger.common.services.network.DebuggerTransport
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

/**
 * Server implementation of DebuggerTransport that waits for incoming connections
 * and reads lines from connected clients, redirecting all data to the incoming channel.
 * Supports multiple concurrent client connections.
 */
class DebuggerSimpleServerTransport(
    val config: DebuggerServerTransportConfig,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : DebuggerTransport<String> {

    companion object {
        private val logger = thisLogger()
    }

    private val _isRunning = AtomicBoolean(false)

    private val _isStarted = MutableStateFlow(false)

    private val _activeClientCount = MutableStateFlow(0)

    private val _clientCounter = AtomicInteger(0)

    // Child scope for client handler coroutines.
    // Uses SupervisorJob so one handler failure doesn't cancel others.
    // Cancelling this scope forcibly stops all handlers on shutdown.
    private val clientScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))

    private var _serverSocket: ServerSocket? = null

    override val name: String = "AI Debugger server transport (port: ${config.port})"

    override val incoming: Channel<String> = Channel(Channel.BUFFERED)

    val isStarted: StateFlow<Boolean>
        get() = _isStarted

    override val isConnected: StateFlow<Boolean> =
        _activeClientCount.map { it > 0 }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        if (config.autoStart) {
            scope.launch {
                start()
            }
        }
    }

    override suspend fun start() {
        if (_isRunning.getAndSet(true)) {
            logger.info("$name. Transport is already running")
            return
        }

        startServer()

        _isStarted.filter { started -> started }.first()
    }

    override suspend fun stop() {
        if (!_isRunning.getAndSet(false)) {
            logger.info("$name. Transport is already stopped")
            return
        }

        _isStarted.filter { started -> !started }.first()
    }

    private fun startServer() = scope.launch {
        logger.info("$name. Starting server...")

        try {
            _serverSocket = ServerSocket(config.port).apply {
                soTimeout = config.acceptTimeout
            }

            _isStarted.value = true

            logger.info("$name. Started successfully. Waiting for connections...")

            while (_isRunning.get()) {
                try {
                    val client = _serverSocket?.accept()
                    if (client != null) {
                        val clientId = _clientCounter.incrementAndGet()

                        logger.info("$name. Received new connection (id: $clientId, remote: ${client.remoteSocketAddress})")

                        _activeClientCount.update { it + 1 }
                        handleClientConnection(client, clientId)
                    }
                } catch (_: SocketTimeoutException) {
                    // Normal timeout, continue loop to check isRunning
                    continue
                } catch (e: IOException) {
                    if (_isRunning.get()) {
                        logger.error("$name. Error accepting connection: ${e.message}", e)
                    }
                    break
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("$name. Error starting server: ${e.message}", e)
        } finally {
            withContext(NonCancellable) {
                stopServer()
            }
        }
    }

    private fun handleClientConnection(client: Socket, clientId: Int) {
        // Launch a separate coroutine for each client connection
        logger.debug { "$name. Launch a read job for new connection (id: $clientId, remote: ${client.remoteSocketAddress})" }

        clientScope.launch {
            var reader: BufferedReader? = null
            try {
                reader = client.getInputStream().bufferedReader()

                // Read until the client closes the connection (EOF).
                // Do NOT check _isRunning here — we must drain all remaining data
                // even after stop() is called, to avoid losing events.
                while (!client.isClosed) {
                    val line = reader.readLine() ?: break
                    incoming.send(line)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_isRunning.get()) {
                    logger.error("$name. Error reading from client (id: $clientId): ${e.message}", e)
                }
            } finally {
                _activeClientCount.update { it - 1 }

                try {
                    reader?.close()
                    client.close()
                } catch (e: Exception) {
                    logger.error("$name. Error closing client (id: $clientId): ${e.message}", e)
                }

                logger.info("$name. Client disconnected (id: $clientId)")
            }
        }
    }

    private suspend fun stopServer() = withContext(Dispatchers.IO) {
        logger.info("$name. Stopping server...")

        // Close server socket first — stop accepting new connections
        try {
            _serverSocket?.close()
        } catch (e: Exception) {
            logger.error("$name. Error closing server socket: ${e.message}", e)
        } finally {
            _serverSocket = null
        }

        // Wait for active client handlers to finish reading remaining data.
        // Handlers read until EOF (Python closes connection) and then
        // decrement _activeClientCount.
        if (_activeClientCount.value > 0) {
            withTimeoutOrNull(5_000) {
                _activeClientCount.filter { it == 0 }.first()
            }
        }

        // Cancel any handlers that didn't finish in time
        clientScope.coroutineContext[Job]?.cancel()

        incoming.close()
        _isRunning.set(false)
        _isStarted.value = false
    }
}