package com.intellij.dataWrangler.jupyterPython.engine.console

import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.scientific.tables.utils.launchIO
import com.jetbrains.python.console.PydevConsoleCommunication

data class DataWranglerLocalFile(val tableVirtualFile: VirtualFile)

interface PyConsoleDataWranglerContext : PythonDataWranglerContext {
  fun getPyFrameAccessor(): PydevConsoleCommunication
  suspend fun updateTableInfo()

  override suspend fun executeCommand(commandCode: String) {
    executeCommandImpl(commandCode)
    updateTableInfo()
  }

  private fun executeCommandImpl(commandCode: String) {
    getPyFrameAccessor().execRaw(commandCode)
  }

  override fun dispose() {
    launchIO {
      executeCommandImpl(getCodeDeletePyVariable())
    }
  }
}