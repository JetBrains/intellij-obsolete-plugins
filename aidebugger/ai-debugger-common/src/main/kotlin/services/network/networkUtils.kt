package com.intellij.aidebugger.common.services.network

import java.net.ServerSocket

fun getFreePort(): Int {
  ServerSocket(0).use { socket ->
    return socket.localPort // OS assigns an available port
  }
}
