package com.intellij.aidebugger.koog.session

import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.remote.client.FeatureMessageRemoteClient
import ai.koog.agents.core.feature.remote.client.config.ClientConnectionConfig
import com.intellij.aidebugger.common.services.network.DebuggerTransport
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.diagnostic.trace
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KoogDebuggerTransport(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val connectionConfig: ClientConnectionConfig,
    private val connectionTimeout: Duration = 30.seconds,
    private val connectionRetryDelay: Duration = 500.milliseconds,
    private val pingDelay: Duration = 1.seconds,
) : DebuggerTransport<FeatureMessage> {

    companion object {
        @JvmStatic
        private val logger = thisLogger()
    }

    private var koogClient: FeatureMessageRemoteClient? = null

    private val _isConnected = MutableStateFlow(false)
    private val _incoming = Channel<FeatureMessage>(Channel.UNLIMITED)

    private var isConnectedJob: Job? = null
    private var receiveMessagesJob: Job? = null
    private var healthCheckJob: Job? = null

    override val name: String = "Koog client transport (url: ${connectionConfig.url})"

    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    override val incoming: Channel<FeatureMessage>
        get() = _incoming

    fun createBaseHttpClient(): HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = connectionTimeout.inWholeMilliseconds
            connectTimeoutMillis = connectionTimeout.inWholeMilliseconds
            socketTimeoutMillis = connectionTimeout.inWholeMilliseconds
        }
    }

    //region Start / Stop

    override suspend fun start() {
        logger.info("$name: Start Koog debugger transport connection logic")

        val koogClient = FeatureMessageRemoteClient(
            connectionConfig = connectionConfig,
            scope = coroutineScope,
            baseClient = createBaseHttpClient()
        ).also { koogClient = it }

        isConnectedJob = startIsConnectedJob(coroutineScope, koogClient)
        receiveMessagesJob = startReceiveMessagesJob(coroutineScope, koogClient)

        // Connection
        val connectionJob = startConnectionJob(coroutineScope, koogClient)

        val isConnectedJobFinished = withTimeoutOrNull(connectionTimeout) {
            connectionJob.join()
            koogClient.isConnected.first { it }
        } != null

        logger.debug { "$name: Is connection job finished: $isConnectedJobFinished" }

        if (!isConnectedJobFinished || !koogClient.isConnected.value) {
            connectionJob.cancel()
            logger.warn("Failed to connect to Koog agent debugger after $connectionTimeout")
        }

        healthCheckJob = startHealthCheck(koogClient, coroutineScope, pingDelay)
    }

    override suspend fun stop() {
        logger.info("Start closing Koog debugger connection transport")

        // Stop the health check (ping) job first to avoid request failures
        healthCheckJob?.cancelAndJoin()

        // Stop the underlying Koog client connection and wait for it to stop
        koogClient?.close()
        isConnected.first { !it }

        // When the underlying connection is closed, proceed with local jobs
        isConnectedJob?.cancelAndJoin()
        receiveMessagesJob?.cancelAndJoin()

        logger.debug { "Koog debugger connection transport closed" }
    }

    suspend fun reconnect() {
        logger.info("$name. Start reconnection")
        stop()
        start()
    }

    //endregion Start / Stop

    //region Private Methods

    private fun startIsConnectedJob(
        coroutineScope: CoroutineScope,
        koogClient: FeatureMessageRemoteClient
    ) : Job {
        return coroutineScope.launch {
            try {
                koogClient.isConnected.collect { isConnected ->
                    _isConnected.value = isConnected
                    logger.trace { "$name: Koog transport connection state update. is connected: $isConnected" }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                logger.error("$name: Unexpected error in isConnected job: ${e.message}", e)
            } finally {
                logger.info("$name: Is connected job is finished")
            }
        }
    }

    private fun startReceiveMessagesJob(
        coroutineScope: CoroutineScope,
        koogClient: FeatureMessageRemoteClient
    ) : Job {
        return coroutineScope.launch {
            try {
                logger.info("$name: Start receiving messages job")
                koogClient.receivedMessages.receiveAsFlow().collect { message ->
                    logger.trace { "$name: Koog transport received message: $message" }
                    _incoming.send(message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("$name: Unexpected error in receive messages job: ${e.message}", e)
            } finally {
                logger.info("$name: Received messages job finished")
            }
        }
    }

    private fun startConnectionJob(
        coroutineScope: CoroutineScope,
        koogClient: FeatureMessageRemoteClient,
    ): Job {
        return coroutineScope.launch(Dispatchers.IO) {
            logger.info("$name: Start connection job")
            var attempt = 1
            while (isActive && !koogClient.isConnected.value) {
                try {
                    // Connect to Koog agent debugger
                    logger.debug { "$name: [$attempt] Attempt connecting to Koog debugger" }
                    koogClient.connect()

                    logger.info("$name: Successfully connected to Koog debugger after $attempt attempts")
                    break
                } catch (t: CancellationException) {
                    println("connectionJob. CANCELLED")
                    throw t
                } catch (t: SSEClientException) {
                    logger.debug(t) {
                        "$name: [$attempt] Connection attempt failed with Client SSE exception."
                    }
                } catch (t: IOException) {
                    logger.debug(t) {
                        "$name: [$attempt] Connection attempt failed with IO Exception"
                    }
                } catch (t: Throwable) {
                    logger.error("$name: Unexpected error connecting to Koog agent debugger", t)
                    throw t
                }

                logger.debug { "$name: Failed to connect to Koog agent debugger. Retrying in $connectionRetryDelay..." }
                delay(connectionRetryDelay)
                attempt++
            }
        }
    }

    private fun startHealthCheck(
        koogClient: FeatureMessageRemoteClient,
        coroutineScope: CoroutineScope,
        pingDelay: Duration = this.pingDelay
    ) : Job {
        return coroutineScope.launch(Dispatchers.IO) {
            logger.info("$name: Start health check job")
            while (isActive) {
                delay(pingDelay)

                try {
                    koogClient.healthCheck()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.debug(e) { "$name: Health check request failed with error: ${e.message}" }
                }
            }
        }
    }

    //endregion Private Methods
}
