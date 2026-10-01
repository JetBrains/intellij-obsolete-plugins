package com.intellij.aidebugger.common.services.network.config

/**
 * Abstract class representing the transport configuration used by the debugger.
 * This configuration provides key properties and methods to define and validate
 * the network connection settings for the debugger transport layer.
 */
abstract class DebuggerNetworkTransportConfig : DebuggerTransportConfig {

    companion object {
        val availablePortsRange = 1025..65535
    }

    /**
     * The host address of the debugger transport.
     */
    abstract val host: String

    /**
     * The port number of the debugger transport.
     */
    abstract val port: Int

    /**
     * The full URL of the debugger transport.
     */
    val url: String
        get() = "$host:$port"

    /**
     * Validates the port number of the debugger transport.
     */
    fun validatePort() {
        require(port in availablePortsRange) {
            "Port <$port> mush be in expected range: $availablePortsRange"
        }
    }
}