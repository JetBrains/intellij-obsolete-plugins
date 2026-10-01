package com.intellij.aidebugger.common.services.network.server

import com.intellij.aidebugger.common.services.network.client.DebuggerClientTransportConfig
import com.intellij.aidebugger.common.services.network.client.DebuggerSimpleClientTransport
import com.intellij.aidebugger.common.services.network.getFreePort
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.io.PrintWriter
import java.net.Socket
import kotlin.time.Duration.Companion.seconds

@Ignore("Return ignore flag for connection tests as they block builds by hanging coroutine. " +
        "Need to be debugged and fixed later.")
class DebuggerSimpleServerTransportTest {

    companion object {
        private val defaultConnectionTimeout = 5.seconds
    }

    @Test
    fun `server start without connected transports`() = runBlocking {
        // Create transport without an auto-start
        val port = getFreePort()
        withServer(port = port, autoStart = false) { server ->

            // Initially should not be connected
            assertFalse(server.isStarted.value)
            assertFalse(server.isConnected.value)

            // Start the transport
            server.start()

            val isServerStarted = withTimeoutOrNull(defaultConnectionTimeout) {
                server.isStarted.first { it }
            } != null

            // Server is started and is ready for connections
            assertTrue(isServerStarted)

            // Still not connected (no client yet)
            assertTrue(server.isStarted.value)
            assertFalse(server.isConnected.value)

            // Stop the transport
            server.stop()

            // Should remain not connected
            assertFalse(server.isStarted.value)
            assertFalse(server.isConnected.value)
        }
    }

    @Test
    fun `server transport should accept client connection`() = runBlocking {

        val port = getFreePort()
        val isServerStarted = MutableStateFlow(false)

        val clientJob = launch {
            isServerStarted.first { it }

            withClient(port = port) { client ->
                client.isConnected.first { it }
            }
        }

        val serverJob = launch {
            withServer(port = port) { server ->
                server.isStarted.first { it }
                isServerStarted.value = true

                // Wait for a connection
                server.isConnected.first { it }

                // Await a client job to finish before stopping the server
                clientJob.join()
            }
        }

        val jobs = listOf(serverJob, clientJob)
        val isFinished = withTimeoutOrNull(defaultConnectionTimeout) {
            jobs.joinAll()
        } != null

        jobs.forEach { it.cancel() }
        assertTrue(isFinished)

        // Note: We don't test disconnection timing as it's implementation-dependent
    }

    @Test
    fun `server transport should receive simple message from client`() = runBlocking {
        val port = getFreePort()
        val testMessage = "Hello"
        val isServerStarted = MutableStateFlow(false)

        // TODO: AIT-170. Update the test when task is finished and a client can send messages to a server.
        //  Note: Replace with [DebuggerSimpleClientTransport] instance as this has a retry mechanism.
        //        Delete [isServerStarted] field.
        val clientJob = launch {
            // Wait for a server to start
            isServerStarted.first { it }

            Socket("localhost", port).use { clientSocket ->
                PrintWriter(clientSocket.getOutputStream(), true).use { writer ->
                    writer.println("Hello")
                }
            }
        }

        val serverJob = launch {
            withServer(port = port) { server ->
                server.isStarted.first { it }

                val messageReadJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    // First, subscribe to incoming messages before a client start to send messages
                    server.incoming.receiveAsFlow().collect { message ->
                        if (message == testMessage) {
                            cancel()
                        }
                    }
                }

                // Notify a client about server ready
                isServerStarted.value = true

                // Await a client job to finish before stopping the server
                messageReadJob.join()
            }
        }

        val jobs = listOf(serverJob, clientJob)
        val isFinished = withTimeoutOrNull(defaultConnectionTimeout) {
            jobs.joinAll()
        } != null

        jobs.forEach { it.cancel() }
        assertTrue(isFinished)
    }

    @Test
    fun `server transport start should be idempotent`() = runBlocking {
        val port = getFreePort()
        val isServerStarted = MutableStateFlow(false)

        val clientJob = launch {
            // Wait for a server to start
            isServerStarted.first { it }

            // Should still work normally
            withClient(port = port) { client ->
                client.isConnected.first { it }
            }
        }

        val serverJob = launch {
            withServer(port = port, autoStart = false) { server ->
                // Start multiple times
                server.start()
                server.start()
                server.start()

                server.isStarted.first { it }
                isServerStarted.value = true

                server.isConnected.first { it }

                // Await a client job to finish before stopping the server
                clientJob.join()
            }
        }

        val jobs = listOf(serverJob, clientJob)
        val isFinished = withTimeoutOrNull(defaultConnectionTimeout) {
            jobs.joinAll()
        } != null

        jobs.forEach { it.cancel() }
        assertTrue(isFinished)
    }

    @Test
    fun `server transport stop should be idempotent`() = runBlocking {
        val port = getFreePort()

        withServer(port = port, autoStart = false) { server ->
            // Stop multiple times
            server.stop()
            server.stop()
            server.stop()

            // Should not throw exceptions
            assertFalse(server.isStarted.value)
            assertFalse(server.isConnected.value)
        }
    }

    @Test
    fun `server transport should handle multiple concurrent connections`() = runBlocking {

        val port = getFreePort()
        val isServerStarted = MutableStateFlow(false)
        val receivedMessages = mutableListOf<String>()

        // Create multiple concurrent connections
        val client1Job = launch {
            // Wait for a server to start
            isServerStarted.first { it }

            Socket("localhost", port).use { clientSocket ->
                PrintWriter(clientSocket.getOutputStream(), true).use { writer ->
                    writer.println("Client1-Message1")
                    writer.println("Client1-Message2")
                }
            }
        }

        val client2Job = launch {
            // Wait for a server to start
            isServerStarted.first { it }

            Socket("localhost", port).use { clientSocket ->
                PrintWriter(clientSocket.getOutputStream(), true).use { writer ->
                    writer.println("Client2-Message1")
                    writer.println("Client2-Message2")
                }
            }
        }

        // Server
        val serverJob = launch {
            withServer(port = port, autoStart = true) { server ->
                server.isStarted.first { it }

                val messageReadJob = launch {
                    // First, subscribe to incoming messages before a client start to send messages
                    server.incoming.receiveAsFlow().collect { message ->
                        receivedMessages.add(message)
                        println("Server received message: $message")
                        if (receivedMessages.size == 4) {
                            cancel()
                        }
                    }
                }

                // Notify a client about server ready
                isServerStarted.value = true

                // Wait for client jobs to finish before stopping the server
                messageReadJob.join()
            }
        }

        val jobs = listOf(serverJob, client1Job, client2Job)
        val isFinished = withTimeoutOrNull(defaultConnectionTimeout) {
            jobs.joinAll()
        } != null

        jobs.forEach { it.cancel() }
        assertTrue(isFinished)

        // Verify all messages were received
        assertEquals(4, receivedMessages.size)
        assertTrue(receivedMessages.contains("Client1-Message1"))
        assertTrue(receivedMessages.contains("Client1-Message2"))
        assertTrue(receivedMessages.contains("Client2-Message1"))
        assertTrue(receivedMessages.contains("Client2-Message2"))
    }

    //region Private Methods

    private suspend fun withServer(
        port: Int,
        autoStart: Boolean = true,
        action: suspend (DebuggerSimpleServerTransport) -> Unit
    ) {
        val connectionConfig = DebuggerServerTransportConfig(port = port, autoStart = autoStart)
        val transport = DebuggerSimpleServerTransport(config = connectionConfig)

        try {
            action(transport)
        } finally {
            transport.stop()
        }
    }

    private suspend fun withClient(
        host: String = "127.0.0.1",
        port: Int,
        autoConnect: Boolean = true,
        action: suspend (DebuggerSimpleClientTransport) -> Unit
    ) {
        val connectionConfig = DebuggerClientTransportConfig(host = host, port = port, autoConnect = autoConnect)
        val transport = DebuggerSimpleClientTransport(config = connectionConfig)

        try {
            action(transport)
        } finally {
            transport.stop()
        }
    }

    //endregion Private Methods
}