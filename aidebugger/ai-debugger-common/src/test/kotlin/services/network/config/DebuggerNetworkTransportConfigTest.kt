package com.intellij.aidebugger.common.services.network.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DebuggerNetworkTransportConfigTest {

    class TestNetworkConfig(
        override val host: String = "test_host",
        override val port: Int = 12345,
    ) : DebuggerNetworkTransportConfig()

    @Test
    fun `test initial configuration state`() {
        val host = "test_host"
        val port = 12345
        val config = TestNetworkConfig(host = host, port = port)

        assertEquals(host, config.host)
        assertEquals(port, config.port)
        assertEquals("$host:$port", config.url)
    }

    @Test
    fun `test validate port for port number in range`() {
        val port = 12345
        val config = TestNetworkConfig(port = port)

        config.validatePort()
        assertEquals(port, config.port)
    }

    @Test
    fun `test validate port for port number out of range`() {
        val port = 123456789
        val config = TestNetworkConfig(port = port)
        assertEquals(port, config.port)

        val throwable = assertThrows(IllegalArgumentException::class.java) {
            config.validatePort()
        }

        assertEquals(
            "Port <$port> mush be in expected range: ${DebuggerNetworkTransportConfig.availablePortsRange}",
            throwable.message
        )

        assertEquals(port, config.port)
    }
}