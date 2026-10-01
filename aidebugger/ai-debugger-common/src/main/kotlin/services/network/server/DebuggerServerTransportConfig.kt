package com.intellij.aidebugger.common.services.network.server

import com.intellij.aidebugger.common.services.network.config.DebuggerNetworkTransportConfig

/**
 * Configuration class for a server-side debugger transport.
 *
 * Provides specific network connection settings required for the debugger server,
 * including host, port, protocol, acceptance timeout, and auto-start behavior.
 *
 * @property port The port number on which the debugger server listens to.
 * @property host The host address of the debugger server. Defaults to "localhost".
 * @property acceptTimeout The timeout in milliseconds for accepting client connections,
 * allowing periodic checks on whether the server is still running. Defaults to 1000 ms.
 * @property autoStart Determines if the server should automatically start when initialized. Defaults to true.
 */
class DebuggerServerTransportConfig(
    override val port: Int,
    override val host: String = "localhost",
    val acceptTimeout: Int = 1000, // Timeout for accept() to allow periodic checking of isRunning
    val autoStart: Boolean = true,
) : DebuggerNetworkTransportConfig()