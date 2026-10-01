package com.intellij.aidebugger.common.services.network.client

import com.intellij.aidebugger.common.services.network.config.DebuggerNetworkTransportConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration class for the Debugger client transport layer.
 * Defines parameters required to establish and manage the client transport's connection
 * to the debugger server.
 *
 * @property host The host address of the debugger server.
 * @property port The port number on the debugger server to connect to.
 * @property retryDelay The time to wait before retrying a failed connection attempt.
 * @property maxRetriesCount The maximum number of connection retry attempts before giving up.
 * @property autoConnect A flag indicating whether the client should attempt to automatically
 * connect to the debugger server upon initialization.
 */
class DebuggerClientTransportConfig(
    override val host: String,
    override val port: Int,
    val retryDelay: Duration = 1.seconds,
    val maxRetriesCount: Int = 10,
    val autoConnect: Boolean = true,
) : DebuggerNetworkTransportConfig()
