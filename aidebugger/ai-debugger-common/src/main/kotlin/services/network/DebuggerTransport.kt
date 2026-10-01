package com.intellij.aidebugger.common.services.network

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for debugger transport implementations.
 * Provides a channel for receiving messages from the debugger.
 */
interface DebuggerTransport<TMessage> where TMessage : Any {

    /**
     * Name of the transport.
     */
    val name: String

    /**
     * Checks if the transport is connected.
     */
    val isConnected: StateFlow<Boolean>

    /**
     * Channel for receiving messages from the debugger.
     */
    val incoming: Channel<TMessage>

    /**
     * Starts the transport connection.
     */
    suspend fun start()

    /**
     * Stops the transport connection.
     */
    suspend fun stop()
}
